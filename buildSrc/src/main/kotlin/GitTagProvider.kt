import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class GitTagProvider : ValueSource<String, ValueSourceParameters.None> {
	@get:Inject
	abstract val execOperations: ExecOperations

	override fun obtain(): String {
		return try {
			val stdout = ByteArrayOutputStream()
			val stdout1 = ByteArrayOutputStream()

			execOperations.exec {
				commandLine("git", "rev-list", "--tags", "--max-count=1")
				standardOutput = stdout
			}
			val lastRev = stdout.toString().trim()
			if (lastRev.isEmpty()) return "Error"

			execOperations.exec {
				commandLine("git", "describe", "--tags", lastRev)
				standardOutput = stdout1
			}
			stdout1.toString().trim()
		} catch (_: Exception) {
			"Error"
		}
	}
}
