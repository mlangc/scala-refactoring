/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package tests.sourcegen

import tests.util.TestHelper
import org.junit.Assert._
import tools.nsc.symtab.Flags
import tools.nsc.ast.parser.Tokens
import scala.reflect.internal.util.BatchSourceFile

class AsinglePrettyPrinterTest extends TestHelper {

  import global._

  implicit class TreePrettyPrintMethods(original: Tree) {
    def prettyPrintsTo(expected: String) = {
      val sourceFile = {
        // we only need the source file to see what kinds of newline we need to generate,
        // so we just pass the expected output :-)
        new BatchSourceFile("noname", expected)
      }
      assertEquals(stripWhitespacePreservers(expected), generate(cleanTree(original), sourceFile = Some(sourceFile)).asText)
    }
  }

  val changeSomeModifiers = transform {
    case t: ClassDef =>
      t.copy(mods = NoMods) setPos t.pos
    case t: DefDef =>
      t.copy(mods = NoMods withPosition (Flags.PROTECTED, NoPosition) withPosition (Flags.METHOD, NoPosition)) setPos t.pos
    case t: ValDef =>
      t.copy(mods = NoMods withPosition (Tokens.VAL, NoPosition)) setPos t.pos
    case t => t
  }

  @Test
  def testClassTemplates() = global.ask { () =>

    val tree = treeFrom("""
    trait ATrait
    class ASuperClass(x: Int, val d: String)
    class AClass(i: Int, var b: String, val c: List[String]) extends ASuperClass(i, b) with ATrait {
      self_type_annotation =>
      def someMethod() = {
      }
    }
    """)

    tree prettyPrintsTo """trait ATrait

class ASuperClass(x: Int, val d: String)

class AClass(i: Int, var b: String, val c: List[String]) extends ASuperClass(i, b) with ATrait {
  self_type_annotation =>
  def someMethod() = ()
}"""
  }
}
