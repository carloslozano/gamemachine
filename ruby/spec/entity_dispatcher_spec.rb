require 'spec_helper'

module GameMachine
  module Systems
    
    describe EntityDispatcher do

      let(:player) {Player.new}
      let(:client_connection) {ClientConnection.new}

      let(:leave_chat) do
        Entity.new.set_leave_chat(LeaveChat.new).
          set_player(player).set_client_connection(client_connection)
      end

      let(:bad_entity) {Entity.new.set_id('1').set_publish(Publish.new)}
      let(:empty_entity) {Entity.new.set_id('1')}

      let(:actor_ref) {mock('ActorRef')}

      describe "aspects" do
        before(:each) do
          SystemManager.registered.each do |klass|
            klass.stub(:find).and_return(actor_ref)
          end
          ActorBuilder.new(EntityDispatcher).with_name('dispatch_test').start
        end

        it "dispatches entities to correct system" do
          client_message = ClientMessage.new.add_entity(leave_chat)
          actor_ref.should_receive(:tell)
          EntityDispatcher.should_receive_message(client_message,'dispatch_test') do
            EntityDispatcher.find('dispatch_test').tell(client_message)
          end
        end

        it "does not dispatch empty entities" do
          client_message = ClientMessage.new.add_entity(empty_entity)
          EntityDispatcher.should_receive_message(client_message,'dispatch_test') do
            EntityDispatcher.find('dispatch_test').tell(client_message)
          end
        end

        it "does not dispatch entities to systems without an aspect" do
          client_message = ClientMessage.new.add_entity(bad_entity)
          EntityDispatcher.should_receive_message(client_message,'dispatch_test') do
            EntityDispatcher.find('dispatch_test').tell(client_message)
          end
        end

      end

    end

  end
end


