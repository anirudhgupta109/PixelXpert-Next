package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import java.lang.reflect.Modifier

internal data class FermatRecordingTargets(
	val prompts: List<Method>,
	val playPrompts: FieldReference,
	val feature: FieldReference,
	val recordingClass: String,
	val skipValue: Boolean,
) {
	companion object {
		fun resolve(apks: List<File>, recordingResourceIds: List<Int>): FermatRecordingTargets {
			require(recordingResourceIds.size == 3 && recordingResourceIds.all { it != 0 })
			val classes = DialerDiscoveryCache.classes(apks)
			val cues = mutableListOf<Method>()
			val recordingClasses = mutableListOf<ClassDef>()
			val readers = mutableMapOf<FieldReference, MutableSet<Method>>()
			for (clazz in classes.values) {
				val resources = mutableSetOf<Int>()
				for (method in clazz.methods) {
					val strings = mutableSetOf<String>()
					for (instruction in (method.implementation?.instructions ?: emptyList())) {
						val ref = (instruction as? ReferenceInstruction)?.reference
						if (ref is StringReference) strings += ref.string
						if (instruction is NarrowLiteralInstruction && instruction.narrowLiteral in recordingResourceIds) {
							resources += instruction.narrowLiteral
						}
						if (instruction.opcode == Opcode.IGET_BOOLEAN && ref is FieldReference) {
							readers.getOrPut(ref) { mutableSetOf() } += method
						}
					}
					if (strings.containsAll(listOf("com/android/dialer/fermat/impl/audioprism/FermatSession",
						"playBeforeDisclaimerAudioCue", "Beep muted."))) cues += method
				}
				if (resources.containsAll(recordingResourceIds)) recordingClasses += clazz
			}
			val cue = cues.single()
			require(cue.returnType == "Ljava/lang/Object;" && cue.parameterTypes.size == 1 &&
				cue.parameterTypes.single().startsWith("L"))
			val session = classes.getValue(cue.definingClass)
			val flag = references(cue).filterIsInstance<FieldReference>().filter {
				it.definingClass == session.type && it.type == "Z" && cue in readers[it].orEmpty()
			}.distinct().single()
			val prompts = readers.getValue(flag).toList()
			require(prompts.size == 3 && prompts.all {
				it.definingClass == session.type && !Modifier.isStatic(it.accessFlags) &&
					it.returnType == cue.returnType && it.parameterTypes == cue.parameterTypes
			})
			val announcements = prompts.filter { it != cue }
			val dispatchers = announcements.map { method ->
				references(method).filterIsInstance<MethodReference>().filter {
					it.definingClass == session.type && it.returnType == "Ljava/lang/Object;" &&
						it.parameterTypes.map { type -> type.toString() } == listOf("I", cue.parameterTypes.single().toString())
				}.toSet()
			}
			require(dispatchers.reduce { a, b -> a intersect b }.size == 1)

			// Verify the skip branch itself. A renamed or inverted flag is fine;
			// a branch with calls, writes or an unknown return value is not.
			val skipValue = listOf(false, true).filter { value ->
				val cueReturn = skipReturn(cue, flag, value)
				val returns = announcements.map { skipReturn(it, flag, value) }
				cueReturn?.definingClass == "Ljava/lang/Boolean;" && cueReturn.name == "TRUE" &&
					cueReturn.type == "Ljava/lang/Boolean;" && returns.all { field ->
						field != null && field.definingClass in classes && field.type == field.definingClass &&
						classes.getValue(field.definingClass).fields.any {
							it == field && Modifier.isStatic(it.accessFlags) && Modifier.isFinal(it.accessFlags)
						}
					}
			}.single()
			val recording = recordingClasses.single()
			val feature = session.fields.filter {
				!Modifier.isStatic(it.accessFlags) && it.type in recording.interfaces
			}.single()
			return FermatRecordingTargets(prompts, flag, feature, recording.type, skipValue)
		}

		private fun references(method: Method) = (method.implementation?.instructions ?: emptyList())
			.mapNotNull { (it as? ReferenceInstruction)?.reference }

		private fun skipReturn(method: Method, flag: FieldReference, value: Boolean): FieldReference? {
			val code = method.implementation ?: return null
			val instructions = code.instructions.toList()
			var address = 0
			val addresses = instructions.map { instruction -> address.also { address += instruction.codeUnits } }
			val indices = addresses.withIndex().associate { it.value to it.index }
			val reads = instructions.indices.filter { index ->
				instructions[index].opcode == Opcode.IGET_BOOLEAN &&
					(instructions[index] as ReferenceInstruction).reference == flag
			}
			if (reads.size != 1) return null
			val readIndex = reads.single()
			val read = instructions[readIndex] as TwoRegisterInstruction
			// All matched methods are instance methods with one reference parameter.
			if (read.registerB != code.registerCount - 2) return null
			val branchIndex = readIndex + 1
			val branch = instructions.getOrNull(branchIndex) ?: return null
			if (branch.opcode !in setOf(Opcode.IF_EQZ, Opcode.IF_NEZ) ||
				(branch as OneRegisterInstruction).registerA != read.registerA) return null
			val taken = if (branch.opcode == Opcode.IF_EQZ) !value else value
			var index = if (taken) indices[addresses[branchIndex] + (branch as OffsetInstruction).codeOffset]
				?: return null else branchIndex + 1
			val registers = mutableMapOf<Int, FieldReference>()
			val visited = mutableSetOf<Int>()
			repeat(16) {
				if (!visited.add(index)) return null
				val instruction = instructions.getOrNull(index) ?: return null
				when (instruction.opcode) {
					Opcode.NOP -> index++
					Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32 -> {
						index = indices[addresses[index] + (instruction as OffsetInstruction).codeOffset] ?: return null
					}
					Opcode.SGET_OBJECT -> {
						registers[(instruction as OneRegisterInstruction).registerA] =
							(instruction as ReferenceInstruction).reference as FieldReference
						index++
					}
					Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16 -> {
						instruction as TwoRegisterInstruction
						registers[instruction.registerA] = registers[instruction.registerB] ?: return null
						index++
					}
					Opcode.RETURN_OBJECT -> return registers[(instruction as OneRegisterInstruction).registerA]
					else -> return null
				}
			}
			return null
		}
	}
}
