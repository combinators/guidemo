package org.combinators.guidemo

import org.combinators.cls.types._
import syntax._

trait SemanticConcepts {
  object Location {
    def apply(of: Type): Type = Constructor("Location", of)
    val Database: Type = Constructor("Database")
    val Logo: Type = Constructor("Logo")
  }

  object ChoiceDialog {
    def apply(formShape: Type): Type = Constructor("ChoiceDialog", formShape)
  }
  object OrderMenu {
    def apply(formShape: Type): Type = Constructor("OrderMenu", formShape)
  }
  object FormShape {
    val RadioButtons: Type = Constructor("RadioButtons")
    val DropDown: Type = Constructor("DropDown")
    val variableFormShape = Variable("alpha")
    lazy val formShapeKinding: Kinding =
      Kinding(variableFormShape)
        .addOption(RadioButtons).addOption(DropDown)
  }


  object BuildFile {
    val Code: Type = Constructor("BuildFile")
    val ExtraDependencies: Type = Constructor("ExtraDependencies")
  }

  val BranchName: Type = Constructor("BranchName")
  val DatabaseAccessCode: Type = Constructor("DatabaseAccessCode")
}
