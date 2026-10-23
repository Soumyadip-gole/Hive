package protocol;

public class Message{
    MessageType type;
    Payload payload;
    public Message(){}
    public Message(MessageType type,Payload payload){
        this.type=type;
        this.payload=payload;
    }
}
