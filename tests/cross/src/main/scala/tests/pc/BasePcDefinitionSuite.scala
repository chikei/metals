package tests.pc

import tests.BasePCSuite
import scala.meta.internal.metals.CompilerOffsetParams
import org.eclipse.lsp4j.TextEdit
import org.eclipse.{lsp4j => l}
import tests.TextEdits
import scala.collection.JavaConverters._
import scala.meta.internal.mtags.MtagsEnrichments._

abstract class BasePcDefinitionSuite extends BasePCSuite {
  def check(
      name: String,
      original: String
  ): Unit = {
    test(name) {
      val noRange = original
        .replaceAllLiterally("<<", "")
        .replaceAllLiterally(">>", "")
      val uri = "A.scala"
      val (code, offset) = params(noRange, uri)
      import scala.meta.inputs.Position
      import scala.meta.inputs.Input
      val offsetRange = Position.Range(Input.String(code), offset, offset).toLSP
      val locations = pc.definition(
        CompilerOffsetParams(uri, code, offset)
      )
      val edits = locations.asScala.toList.flatMap { location =>
        if (location.getUri() == uri) {
          List(
            new TextEdit(
              new l.Range(
                location.getRange().getStart(),
                location.getRange().getStart()
              ),
              "<<"
            ),
            new TextEdit(
              new l.Range(
                location.getRange().getEnd(),
                location.getRange().getEnd()
              ),
              ">>"
            )
          )
        } else {
          val filename = location.getUri()
          val comment = s"/*$filename*/"
          if (code.contains(comment)) {
            Nil
          } else {
            List(new TextEdit(offsetRange, comment))
          }
        }
      }
      val obtained = TextEdits.applyEdits(code, edits)
      val expected = original.replaceAllLiterally("@@", "")
      assertNoDiff(obtained, expected)
    }
  }
}