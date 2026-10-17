package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import java.lang.reflect.Modifier

internal data class AutomaticRecordingTargets(
    val eligibility: Method,
    val configConstructor: Method,
    val contacts: FieldReference,
    val configFactories: List<Method>,
    val feature: FieldReference,
    val recordingClass: String,
) {
    companion object {
        fun resolve(apks: List<File>, resourceIds: List<Int>): AutomaticRecordingTargets {
            require(resourceIds.size == 3 && resourceIds.all { it != 0 })
            val classes = DialerDiscoveryCache.classes(apks)
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
            val contacts = method.implementation!!.instructions.filter { it.opcode == Opcode.IGET_OBJECT }
                .map { (it as ReferenceInstruction).reference }.filterIsInstance<FieldReference>()
                .filter { it.type == "Ljava/util/Set;" }.distinct().single()
            val config = classes.getValue(contacts.definingClass)
            require(config.methods.any { candidate -> candidate.implementation?.instructions?.any {
                ((it as? ReferenceInstruction)?.reference as? StringReference)?.string ==
                    "AutoActivationConfig(autoActivationSettings="
            } == true })
            val constructor = config.methods.filter { it.name == "<init>" }.single()
            require(constructor.parameterTypes.size == 1 && constructor.parameterTypes.single().startsWith("L"))
            require(constructor.implementation!!.instructions.any {
                it.opcode == Opcode.IPUT_OBJECT && (it as ReferenceInstruction).reference == contacts
            })
            val factories = classes.values.flatMap { it.methods }.filter { candidate ->
                candidate.implementation?.instructions?.any {
                    it.opcode == Opcode.INVOKE_DIRECT && (it as ReferenceInstruction).reference == constructor
                } == true
            }
            require(factories.isNotEmpty() && factories.all {
                !Modifier.isStatic(it.accessFlags) && it.returnType == "Ljava/lang/Object;" &&
                    it.parameterTypes.size == 1 && it.parameterTypes.single().startsWith("L")
            })
            val recording = classes.getValue(recordingClasses.single())
            val feature = classes.getValue(method.definingClass).fields.filter {
                !Modifier.isStatic(it.accessFlags) && it.type in recording.interfaces
            }.single()
            return AutomaticRecordingTargets(method, constructor, contacts, factories, feature, recording.type)
        }
    }
}
