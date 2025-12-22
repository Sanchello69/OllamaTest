import com.rtfparserkit.parser.IRtfListener
import com.rtfparserkit.parser.IRtfSource
import com.rtfparserkit.parser.RtfStreamSource
import com.rtfparserkit.parser.standard.StandardRtfParser
import com.rtfparserkit.rtf.Command
import java.io.File
import java.io.FileInputStream

class RtfParser {
    fun parseRtfFile(filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }

        val textBuilder = StringBuilder()

        FileInputStream(file).use { inputStream ->
            val source: IRtfSource = RtfStreamSource(inputStream)
            val parser = StandardRtfParser()

            val listener = object : IRtfListener {
                override fun processString(text: String) {
                    textBuilder.append(text)
                }

                override fun processDocumentStart() {}
                override fun processDocumentEnd() {}
                override fun processGroupStart() {}
                override fun processGroupEnd() {}
                override fun processBinaryBytes(data: ByteArray) {}
                override fun processCharacterBytes(data: ByteArray) {}
                override fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {}
            }

            parser.parse(source, listener)
        }

        return textBuilder.toString()
    }
}
