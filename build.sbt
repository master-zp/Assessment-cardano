ThisBuild / scalaVersion := "3.3.8"
ThisBuild / version := "0.1.0"

// NOTE: pin to whatever `sbt new scalus3/hello.g8` generates for you if it differs.
val scalusVersion = "1.4.0"

lazy val root = (project in file("."))
    .settings(
      name := "scalus-vesting",
      scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
      // The compiler plugin turns @Compile Scala code into Scalus IR and then UPLC.
      addCompilerPlugin("org.scalus" % "scalus-plugin" % scalusVersion cross CrossVersion.full),
      libraryDependencies ++= Seq(
        "org.scalus" %% "scalus" % scalusVersion,
        "org.scalus" %% "scalus-cardano-ledger" % scalusVersion,
        "org.scalus" %% "scalus-testkit" % scalusVersion % Test,
        "org.scalatest" %% "scalatest" % "3.2.19" % Test
      )
    )
