This is a poor-man rake alternative for scala scripts (ammonite).

## Getting started
- download 2 files `TaskRunner.sc` and `tasks.sc` to wherever desired, for example to a project dir
- edit `tasks.sc` to add the real tasks.

## Usage example
```bash
amm tasks.sc # list available tasks and description
amm tasks.sc abc # run task `abc`
amm tasks.sc abc a_arg_def=123 # run task `abc` with argument
```

## Why?
I use rake as entry point for almost anything that requires running a command
in terminal. It serves as a task runner, and also as some minimal
documentation. In the simplest case the Rakefile looks like this:

```ruby
require "#{Dir.home}/helpers/rake-utils.rb"
desc 'do something'
task 'abc' do |task|
  log task
  sh "echo Hello"
end
```

which is obviously more verbose than just `echo Hello`, however it has some great
benefits:
- I don't have to remember the exact command; just run `rake` to get started
- running `rake` without arguments will show all available tasks with description like this:
```
rake
rake ls  # list available tasks
rake abc # do something
```
- auto-completion works well for rake, so I can just type `rake a<Tab>` and
  it's faster than typing `echo Hello`

I like this approach so much that I miss it badly when I have some projects in
scala script. I can use rake there, but it feels so wrong. So I made this
simple project to scratch my own itch.
