package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import java.lang.reflect.Modifier

internal data class AutomaticRecordingTargets(
    val eligibility: Method,
    val contains: MethodReference,
    val feature: FieldReference,
    val recordingClass: String,
) {
    companion object {
        fun resolve(apks: List<File>, resourceIds: List<Int>): AutomaticRecordingTargets {
            require(resourceIds.size == 3 && resourceIds.all { it != 0 })
            val classes = apks.flatMap { apk ->
                val dex = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault())
                dex.dexEntryNames.flatMap { requireNotNull(dex.getEntry(it)).dexFile.classes }
            }.associateBy { it.type }
            val eligibility = mutableListOf<Method>()
            val recordingClasses = mutableListOf<String>()
            for (clazz in classes.values) {
                val resources = mutableSetOf<Int>()
                for (method in clazz.methods) {
                    val strings = mutableSetOf<String>()
                    for (instruction in method.implementation?.instructions ?: emptyList()) {
                        val ref = (instruction as? ReferenceInstruction)?.reference
                        if (ref is StringReference) strings += ref.string
                        if (instruction is NarrowLiteralInstruction && instruction.narrowLiteral in resourceIds) {
                            resources += instruction.narrowLiteral
                        }
                    }
                    if (strings.containsAll(listOf(
                        "com/android/dialer/fermat/impl/incall/FermatAutomaticActivationEligibility",
                        "isEligibleForAutomaticActivation",
                        "autoActivateForSelectedContacts: %s, isNumberEligible: %s",
                        "autoActivateForNonContacts: %s",
                        "Fermat not enabled; ineligible for automatic activation",
                        "Call not eligible for Fermat",
                        "Not within geofence",
                        "Required audio not downloaded; ineligible for automatic activation",
                    ))) eligibility += method
                }
                if (resources.containsAll(resourceIds)) recordingClasses += clazz.type
            }
            val method = eligibility.single()
            require(!Modifier.isStatic(method.accessFlags) && method.returnType == "Ljava/lang/Object;" &&
                method.parameterTypes.size == 1 && method.parameterTypes.single().startsWith("L"))
            val calls = method.implementation!!.instructions.filter { it.opcode == Opcode.INVOKE_STATIC }
                .map { (it as ReferenceInstruction).reference }.filterIsInstance<MethodReference>()
            val contains = calls.filter {
                it.returnType == "Z" && it.parameterTypes.map { p -> p.toString() } ==
                    listOf("Ljava/lang/Iterable;", "Ljava/lang/Object;")
            }.single()
            val helper = classes.getValue(contains.definingClass).methods.single { it == contains }
            require(Modifier.isStatic(helper.accessFlags))
            require(helper.implementation!!.instructions.any {
                val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
                it.opcode == Opcode.INVOKE_INTERFACE && ref?.definingClass == "Ljava/util/Collection;" &&
                    ref.name == "contains" && ref.returnType == "Z" &&
                    ref.parameterTypes.map { p -> p.toString() } == listOf("Ljava/lang/Object;")
            })
            val recording = classes.getValue(recordingClasses.single())
            val feature = classes.getValue(method.definingClass).fields.filter {
                !Modifier.isStatic(it.accessFlags) && it.type in recording.interfaces
            }.single()
            return AutomaticRecordingTargets(method, contains, feature, recording.type)
        }
    }
}
