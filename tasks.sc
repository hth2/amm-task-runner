// this file is a skeleton for tasks.sc (poor-man Rakefile alternative in scala)
// copy this file to desired location and change the path to TaskRunner.sc accordingly

import $file.TaskRunner, TaskRunner._, TaskRunner.Tasks._

Tasks {
  Task("1", desc = "This is task 1", deps = "2", "3") {
    println("1")
  }

  Task("2", "This is task 2", deps = "3") {
    println("2")
  }

  Task("3", "This is task 3") {
    println("3")
    for ((k, v) <- taskArgs)
      println(s"key=$k, val=$v")
  }

  setDefaultTask("1") // if omit will run built-in task "ls", which lists all tasks
}

@main
def main(args: String*) =  run(args)
