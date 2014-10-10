ENV['APP_ROOT'] ||= File.expand_path(Dir.pwd)
ENV['JAVA_ROOT'] = File.join(ENV['APP_ROOT'],'java','project')
ENV['GAME_ENV'] = 'test'
require 'rubygems'

require 'java'
policyfile = File.join(ENV['APP_ROOT'],'config','app.policy')
java.lang.System.setProperty("java.security.policy", policyfile)

begin
  require 'game_machine'
rescue LoadError
  require_relative '../lib/game_machine'
end

java.lang.System.setSecurityManager(GameMachine::JavaLib::CodeblockSecurityManager.new)