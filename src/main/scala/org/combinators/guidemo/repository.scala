package org.combinators.guidemo

import java.net.URL
import java.nio.file.Paths

import com.github.javaparser.ast.CompilationUnit
import org.combinators.cls.git.{EmptyInhabitationBatchJobResults, ResultLocation, Results}
import org.combinators.cls.interpreter.{ReflectedRepository, combinator}
import org.combinators.cls.types.syntax._
import org.combinators.cls.types.{Kinding, Omega, Type, Variable}
import org.combinators.guidemo.Helpers._
import org.combinators.guidemo.domain.{CoffeeBar, DatabaseType, MenuLayout}
import org.combinators.templating.persistable.BundledResource
import org.combinators.templating.persistable.JavaPersistable._
import org.combinators.templating.twirl.Java

import scala.meta._

class Repository(coffeeBar: CoffeeBar) extends SemanticConcepts {
  lazy val kinding = FormShape.formShapeKinding

  @combinator object customerForm {
    def apply(title: String,
      logoLocation: URL,
      optionSelector: CoffeeBarModifier): CompilationUnit = {
      val coffeeBar = Java(readFile("CustomerForm.java")).compilationUnit()
      addOptionSelection(coffeeBar, optionSelector)
      addTitle(coffeeBar, title)
      addLogo(coffeeBar, logoLocation)
      coffeeBar
    }
    val semanticType: Type =
      BranchName =>: Location(Location.Logo) =>: ChoiceDialog(FormShape.variableFormShape) =>: OrderMenu(FormShape.variableFormShape)
  }

  @combinator object projectBuildFile {
    def apply(extraDependencies: Seq[scala.meta.Term]): scala.meta.Source = {
      val buildFile = scala.meta.dialects.Sbt1(readFile("build.sbt")).parse[scala.meta.Source].get
      buildFile.copy(buildFile.children.map {
        case q"libraryDependencies ++= Seq(..$oldDependencies)" =>
          q"""libraryDependencies ++= Seq(..${oldDependencies ++ extraDependencies.toList})"""
        case q"$x" => x
      })
    }
    val semanticType: Type = BuildFile.ExtraDependencies =>: BuildFile.Code
  }


  @combinator object branchName {
    def apply: String = coffeeBar.getBranchName
    val semanticType: Type = BranchName
  }
  @combinator object databaseLocation {
    def apply: String = coffeeBar.getProductDatabase.getDatabaseLocation
    val semanticType: Type = Location(Location.Database)
  }
  @combinator object logoLocation {
    def apply: URL = coffeeBar.getLogoLocation
    val semanticType: Type  = Location(Location.Logo)
  }


  object restJSONProductProvider {
    def apply(databaseLocation: String): CoffeeBarModifier =
      coffeBar => new Runnable() {
        def run() = {
          coffeBar.addImport("java.util.List")
          coffeBar.addImport("java.util.ArrayList")
          coffeBar.addImport("com.fasterxml.jackson.databind.ObjectMapper")


          val cls = coffeBar.getClassByName("CustomerForm").get
          val productsMethod =
            Java(
              s"""
                 |public List<String> getProductOptions() {
                 |    ObjectMapper mapper = new ObjectMapper();
                 |    List<String> options;
                 |    try{
                 |        options = mapper.readValue(
                 |        new URL("$databaseLocation"),
                 |        mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                 |    } catch (Exception e) {
                 |        options = new ArrayList<>();
                 |        JOptionPane.showMessageDialog(this, String.format("Could not load options: %s", e.getMessage()));
                 |    }
                 |    return options;
                 |}
               """.stripMargin).methodDeclarations().head

          cls.addMember(productsMethod)
        }
      }

    val semanticType: Type = Location(Location.Database) =>: DatabaseAccessCode
  }

  object jdbcProductProvider {
    def apply(databaseLocation: String): CoffeeBarModifier =
      coffeBar => new Runnable() {
        def run() = {
          coffeBar.addImport("java.sql.*")
          coffeBar.addImport("java.util.List")
          coffeBar.addImport("java.util.ArrayList")

          val cls = coffeBar.getClassByName("CustomerForm").get
          val productsMethod =
            Java(
              s"""
                 |public List<String> getProductOptions() {
                 |    List<String> options = new ArrayList<>();
                 |    Connection connection = null;
                 |    try{
                 |        Class.forName("org.h2.Driver");
                 |        connection = DriverManager.getConnection("$databaseLocation", "sa", "");
                 |        ResultSet results = connection.prepareStatement("SELECT name FROM coffee").executeQuery();
                 |        while (results.next()) {
                 |            options.add(results.getString("name"));
                 |        }
                 |    } catch (Exception e) {
                 |        JOptionPane.showMessageDialog(this, String.format("Could not load options: %s", e.getMessage()));
                 |    } finally {
                 |        if (connection != null) {
                 |            try {
                 |                connection.close();
                 |            } catch (Exception ex) {
                 |                throw new RuntimeException(ex);
                 |            }
                 |        }
                 |    }
                 |    return options;
                 |}
               """.stripMargin).methodDeclarations().head

          cls.addMember(productsMethod)
        }
      }

    val semanticType: Type = Location(Location.Database) =>: DatabaseAccessCode
  }

  @combinator object dropDownSelector {
    def apply(databaseAccessCode: CoffeeBarModifier): CoffeeBarModifier = {
      coffeBar => new Runnable() {
        def run() = {
          addDatabaseAccessCode(coffeBar, databaseAccessCode)
          val cls = coffeBar.getClassByName("CustomerForm").get
          val initMethod = cls.getMethodsByName("initComponents").get(0)
          val getOrders =
            Java(
              s"""
                 |List<String> options = getProductOptions();
                 |JComboBox optionBox = new JComboBox(options.toArray(new String[0]));
                 |if (options.size() > 0) {
                 |    optionBox.setSelectedIndex(0);
                 |    selectedOrder = options.get(0);
                 |    optionBox.addActionListener(e -> { selectedOrder = (String)optionBox.getSelectedItem(); });
                 |}
                 |this.add(optionBox);
               """.stripMargin).statements()

          getOrders.reverse.foreach(stmt => initMethod.getBody.get().addStatement(0, stmt))
        }
      }
    }
    val semanticType: Type =
      DatabaseAccessCode =>: ChoiceDialog(FormShape.DropDown)
  }

  object jsonDependencies {
    def apply: Seq[scala.meta.Term] =
      q"""Seq(
            "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.8",
            "com.fasterxml.jackson.core" % "jackson-core" % "2.8.8",
            "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8"
         )""" match { case q"Seq(..$xs)" => xs }

    val semanticType: Type = BuildFile.ExtraDependencies
  }

  object jdbcDependencies {
    def apply: Seq[scala.meta.Term] =
      q"""Seq(
           "com.h2database" % "h2" % "1.4.200"
         )""" match { case q"Seq(..$xs)" => xs }

    val semanticType: Type = BuildFile.ExtraDependencies
  }

  @combinator object radioButtonSelector {
    def apply(databaseAccessCode: CoffeeBarModifier): CoffeeBarModifier = {
      coffeBar => new Runnable() {
        def run() = {
          addDatabaseAccessCode(coffeBar, databaseAccessCode)
          val cls = coffeBar.getClassByName("CustomerForm").get
          val initMethod = cls.getMethodsByName("initComponents").get(0)
          val getOrders =
            Java(
              s"""
                 |List<String> options = getProductOptions();
                 |ButtonGroup group = new ButtonGroup();
                 |for (String option : options) {
                 |  JRadioButton optionButton = new JRadioButton(option);
                 |  optionButton.addActionListener(e -> { selectedOrder = option; });
                 |  group.add(optionButton);
                 |  this.add(optionButton);
                 |}
                 |if (options.size() > 0) {
                 |    selectedOrder = options.get(0);
                 |}
                 """.stripMargin).statements()
          getOrders.reverse.foreach(stmt => initMethod.getBody.get().addStatement(0, stmt))
        }
      }
    }
    val semanticType: Type =
      DatabaseAccessCode  =>: ChoiceDialog(FormShape.RadioButtons)
  }

  private def addDatabaseCombinators(repository: ReflectedRepository[Repository]): ReflectedRepository[Repository] = {
    coffeeBar.getProductDatabase.getDatabaseType match {
      case DatabaseType.JDBC =>
        repository
          .addCombinator(jdbcProductProvider)
          .addCombinator(jdbcDependencies)
      case DatabaseType.RestJSON =>
        repository
          .addCombinator(restJSONProductProvider)
          .addCombinator(jsonDependencies)
    }
  }

  private def semanticTarget: Type = {
    OrderMenu(coffeeBar.getMenuLayout match {
      case MenuLayout.DropDown => FormShape.DropDown
      case MenuLayout.RadioButtons => FormShape.RadioButtons
      case MenuLayout.Any => Omega
    })
  }

  def forInhabitation: ReflectedRepository[Repository] = {
    val repo = ReflectedRepository(
        this,
        classLoader = this.getClass.getClassLoader,
        substitutionSpace = this.kinding
      )
    addDatabaseCombinators(repo)
  }

  def getResults(implicit resultLocation: ResultLocation): Results = {
    EmptyInhabitationBatchJobResults(this.forInhabitation)
      .addJob[CompilationUnit](semanticTarget)
      .addJob[scala.meta.Source](BuildFile.Code)
      .compute()
      .addExternalArtifact(BundledResource("gitignore", Paths.get(".gitignore"), getClass))
      .addExternalArtifact(BundledResource("build.properties", Paths.get("project", "build.properties"), getClass))
  }
}
