require 'json'
require 'sinatra/base'
require 'rack-flash'
require 'sinatra/multi_route'

require_relative 'controllers/base_controller'
require_relative 'controllers/index_controller'
require_relative 'controllers/messages_controller'
require_relative 'controllers/auth_controller'
require_relative 'controllers/log_controller'
require_relative 'controllers/player_register_controller'


class WebApp < Sinatra::Base
  CONTROLLERS = {}
  set :bind, GameMachine::Application.config.http_host
  set :port, GameMachine::Application.config.http_port
  set :root, File.expand_path(File.dirname(__FILE__))
  set :environment, :development
  mime_type :proto, 'application/octet-stream'

  register Sinatra::MultiRoute
  enable :sessions
  use Rack::Flash

  def base_uri
    host =  GameMachine::Application.config.http_host
    port = GameMachine::Application.config.http_port
    "http://#{host}:#{port}"
  end

  def controller(name)
    case name
    when :index
      CONTROLLERS[name] ||= Web::Controllers::IndexController.new
    when :auth
      CONTROLLERS[name] ||= Web::Controllers::AuthController.new
    when :messages
      CONTROLLERS[name] ||= Web::Controllers::MessagesController.new
    when :log
      CONTROLLERS[name] ||= Web::Controllers::LogController.new
    when :player_register
      CONTROLLERS[name] ||= Web::Controllers::PlayerRegisterController.new
    end
  end

  set :views, ['web/views', 'games/moba/web/views']

  helpers do
    def find_template(views, name, engine, &block)
      Array(views).each { |v| super(v, name, engine, &block) }
    end
  end

  get '/player_register' do
    @content = {}
    haml :player_register, :layout => :register_layout
  end

  post '/player_register.html' do
    @content = controller(:player_register).set_request(request,params).create
    if @content['error']
      haml :player_register, :layout => :register_layout
    else
      haml :player_registered, :layout => :register_layout
    end
  end

  post '/player_register.json' do
    content = controller(:player_register).set_request(request,params).create
    JSON.generate(content)
  end

  get '/' do
    if request.params['restarted']
      @restarted = true
    end
    haml :index
  end

  get '/alive' do
    JSON.generate({})
  end

  get '/restart' do
    haml :restart
  end

  get '/logs' do
    @logtypes = {
      :app => 'Application',
      :stdout => 'Standard out',
      :stderr => 'Standard error'
    }
    @logtype = (params['logtype'] || 'app').to_sym
    @content = controller(:log).set_request(request,params).logs(@logtype)
    haml :logs
  end

  get '/messages/game' do
    @content = controller(:messages).set_request(request,params).game
    @messages = :game
    haml :game_messages
  end

  post '/messages/game' do
    @content = controller(:messages).set_request(request,params).update
    if @content == 'restart'
      haml :restart
    else
      @messages = :game
      haml :game_messages
    end
  end

  get '/messages/all' do
    @content = controller(:messages).set_request(request,params).all
    @messages = :all
    @format = params['format']
    if @format == 'proto'
      content_type :proto
      attachment 'messages.proto'
      @content
    else
      haml :game_messages
    end
  end

  route :get, :post, '/auth' do
    res = controller(:auth).set_request(request,params).get
    if res == 'error'
      status 403
      body res
    else
      res
    end
  end

  if ENV['MOBA_GAME']
    Moba::Web::App.configure(self)
  end
end

WebApp.run!
