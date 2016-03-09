/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package analysis

import collection.mutable.ListBuffer
import collection.mutable.HashSet
import annotation.tailrec

/**
 * Provides an implementation of the Indexes.IndexLookup trait
 * by combining various CompilationUnitIndexes. Note that creating
 * the GlobalIndex is cheap, all the compilation units were already
 * indexed, and all further work is only done on demand.
 *
 */
trait GlobalIndexes extends Indexes with DependentSymbolExpanders with CompilationUnitIndexes with common.EnrichedTrees with common.InteractiveScalaCompiler with common.TreeTraverser {

  import global._
  import scala.tools.refactoring.util.UnionFind

  object GlobalIndex {

    def apply(compilationUnits: List[CompilationUnitIndex]): IndexLookup =
      new GlobalIndex with
          ExpandGetterSetters with
          SuperConstructorParameters with
          Companion with
          LazyValAccessor with
          OverridesInSuperClasses with
          ClassVals with
          CaseClassVals {

            val cus = compilationUnits
          }

    def apply(t: Tree): IndexLookup = apply(List(CompilationUnitIndex(t)))
  }

  val EmptyIndex = GlobalIndex(Nil)

  trait GlobalIndex extends IndexLookup {

    this: SymbolExpander =>

    def cus(): List[CompilationUnitIndex]

    def declaration(s: Symbol): Option[DefTree] = {
      cus.flatMap(_.definitions.get(s)).flatten.headOption
    }

    def references(s: Symbol) = {
      val decls = declaration(s).toList
      val occs = occurences(s)
      occs filterNot decls.contains
    }

    def rootsOf(trees: List[Tree]) = {
      (for {
        cu <- cus
        tree <- trees
        if cu.root.pos.source.file == tree.pos.source.file
      } yield {
        cu.root
      }).distinct
    }

    @tailrec
    private def linkSymbols(uf: UnionFind[Symbol], syms:List[Symbol], seen:HashSet[Symbol]): UnionFind[Symbol] = {
      if (syms.nonEmpty){
        val nextSymbols = ListBuffer[Symbol]()
        for (s <- syms) {
          for (es <- expand(s) filterNot (_ == NoSymbol)){
            uf.union(s, es)
            if (!seen(es)) nextSymbols += es
          }
          seen += s
        }
        linkSymbols(uf,nextSymbols.toList, seen)
      } else uf
    }

    /*
     * This stores knows symbols into connected components, where the connection is to refer to the same symbol name
     */
    private lazy val symbolsUF: UnionFind[Symbol] = context("Linking symbols") {
      linkSymbols(new UnionFind(), allSymbols(), new HashSet[Symbol]())
    }

    def expandSymbol(s: Symbol): List[Symbol] = symbolsUF.equivalenceClass(s)

    def occurences(s: global.Symbol) = {
      val expandedSymbol = expandSymbol(s)

      expandedSymbol.flatMap { sym =>

        val decs = declaration(sym).toList
        val refs = cus.flatMap { cu =>
          cu.references.get(sym).toList.flatten
        }

        decs ::: refs

      }.filter {

        // see SI-6141
        case t: Ident => t.pos.isRange

        // Calls to super.xy are This trees with a ClassSymbol, but we don't
        // want to see them when looking for occurrences of the ClassSymbol,
        // so we filter them here.
        case t: This if t.pos.isRange =>
          val src = t.pos.source.content.slice(t.pos.start, t.pos.end).mkString
          src != "super"

        case t if t.pos.isTransparent =>
          // We generally want to skip transparent positions,
          // so if one of the children is an opaque range, we
          // skip this tree.
          children(t).exists(_.pos.isOpaqueRange)

        case t =>
          t.pos.isOpaqueRange
      }.distinct
    }

    def allDefinedSymbols = cus.flatMap(_.definitions.keys)

    def allSymbols = cus.flatMap(cu => cu.definitions.keys ++ cu.references.keys)

    def positionToSymbol(p: global.Position): List[global.Symbol] = {

      val hasTreeWithPos: ((global.Symbol, List[global.Tree])) => List[global.Symbol] = {
        case (sym, trees) if trees.exists(_.pos == p) =>
            List(sym)
        case _ =>
            Nil
      }

      cus.flatMap { cu =>
        cu.definitions.flatMap(hasTreeWithPos) ++ cu.references.flatMap(hasTreeWithPos)
      }.filter(_ != NoSymbol).distinct
    }
  }
}
