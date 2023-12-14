import scala.collection.mutable
import scala.io.StdIn

val mTasks = new mutable.LinkedHashMap[String, Task]

object Tasks {
  private var mDefaultTask: Task = _
  private val mArgs: mutable.Map[String, String] = new mutable.LinkedHashMap[String, String]

  def apply(block: => Unit): Unit = {
    mTasks("ls") = new Task("ls", "List all tasks", deps = null, () => printTasks())
    setDefaultTask("ls")
    block
    checkCyclicDeps()
  }

  private def printTasks(): Unit = {
    var maxLen = 0
    for (k <- mTasks.keys)
      if (k.length > maxLen)
        maxLen = k.length
    println("Available tasks:")
    for (k <- mTasks.keys.toArray) { // print in orig. order
      val padding = " ".repeat(maxLen - k.length)
      val task = mTasks(k)
      val depsStr = if (task.deps == null || task.deps.isEmpty) "" else s", deps: ${task.deps.mkString(", ")}"
      println(s"$k:$padding ${task.desc}$depsStr")
    }

    println("""
              |To run one or more tasks, invoke:
              |amm tasks.sc task-1 task-2 ...
              |""".stripMargin)
  }

  def setDefaultTask(taskName: String): Unit = {
    checkTaskName(taskName)
    mDefaultTask = mTasks(taskName)
  }

  def run(args: Seq[String]): Unit = {
    //    println(s"run: ${args.mkString(",")}")
    // treat a_abc=def as argument, not as task name
    val pat = """a_(\w+)=(\S+)""".r
    val tasks = new mutable.ArrayBuffer[String]
    args.foreach {
      case pat(name, value) => mArgs.put(name, value)
      case taskName => tasks.append(taskName)
    }

    if (tasks.isEmpty)
      runTask(mDefaultTask.name)
    else
      tasks.foreach(runTask)
  }

  def taskArgs: Map[String, String] = mArgs.toMap

  def runTasks(taskNames: String*): Unit = {
    taskNames.foreach(runTask)
  }

  def runTask(taskName: String): Unit = {
    try {
      checkTaskName(taskName)
      val task = mTasks(taskName)
      println(s"start: task '$taskName' = ${task.desc}")
      if (task.deps != null)
        task.deps.foreach(runTask)
      task.block()
      println(s"finished: task '$taskName'")
    }
    catch {
      case e: Exception => print(s"error during running task '$taskName': ${e.getMessage}")
        sys.exit(1)
    }
  }

  def checkTaskName(taskName: String): Unit = {
    if (!mTasks.contains(taskName))
      throw new RuntimeException(s"task '$taskName' does not exists!")
  }

  private def checkCyclicDeps(): Unit = {
    val visited = mutable.Set[Task]()

    def searchTask(task: Task): Unit = {
      for (dep <- task.getDeps) {
        if (!visited.contains(dep)) {
          visited.add(dep)
          searchTask(dep)
        }
      }
    }

    for ((_, task) <- mTasks) {
      visited.clear()
      searchTask(task)
      if (visited.contains(task))
        throw new RuntimeException(s"cyclic deps detected: task '${task.name}'")
    }
  }
}

case class Task(name: String, desc: String, deps: Seq[String], block: () => Unit) {
  def getDeps: Seq[Task] = {
    if (deps == null || deps.isEmpty)
      Seq()
    else
      deps.map(taskName => {
        Tasks.checkTaskName(taskName)
        mTasks(taskName)
      })
  }
}

def procToStr(proc: os.proc): String = {
  proc.command.map(s => s.value.mkString("")).mkString(" ")
}

def trySelfUpdate(srcDir: os.Path): Unit = {
  val myPath = os.Path(sourcecode.File())
  if (updateFileFrom(srcDir, myPath, targetMustExists = true)) {
    prependText(myPath,
      s"// this file was copied from $srcDir",
      "// do not edit!"
    )
    os.perms.set(myPath, "r--r--r--")
    println(s"$myPath updated, exit now. Please rerun ammonite")
    sys.exit(0)
  }
}

// if file is older than its counterpart in depDir, copy from there
def updateFileFrom(fromDir: os.Path, target: os.Path, targetMustExists: Boolean = false): Boolean = {
  val baseName = target.last
  val src = fromDir / baseName

  if (!os.exists(src)) {
    println(s"$src does not exist")
    return false
  }
  if (targetMustExists && !os.exists(target)) {
    println(s"$target does not exist")
    return false
  }
  if (os.stat(src).mtime.toMillis > os.stat(target).mtime.toMillis) {
    val answer = getInput(
      s"""
         |file $target is older than its dependency $src
         |copy $src to $target?""".stripMargin,
      "y"
    )
    if (answer == "y") {
      os.copy.over(src, target)
      return true
    }
  }

  false
}

def prependText(file: os.Path, lines: String*): Unit = {
  val sb = new mutable.StringBuilder
  for (line <- lines) {
    sb.append(line)
    sb.append("\n")
  }
  sb.append(os.read(file))
  os.write.over(file, sb.toString)
}


object Task {
  def apply(name: String, desc: String, deps: String*)(block: => Unit): Unit = {
    if (mTasks.contains(name))
      throw new RuntimeException(s"task '$name' already exists!")
    mTasks(name) = new Task(name, desc, deps, () => block)
  }
}

def getInput(prompt: String, defaultAnswer: String = ""): String = {
  print(if (defaultAnswer.isEmpty) prompt else s"$prompt [$defaultAnswer]")
  val input = StdIn.readLine()
  if (input.trim.isEmpty)
    defaultAnswer
  else
    input.trim
}

//class ProcWrapper extends os.proc {
//  proc.command.map(s => s.value.mkString("")).mkString(" ")
//}