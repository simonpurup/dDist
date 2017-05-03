package Project;

public class TextRemoveEvent extends MyTextEvent {

	private int length;
	
	public TextRemoveEvent(int offset, int length) {
		super(offset);
		this.length = length;
	}

	public int getLength() { return length; }

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TextRemoveEvent) {
			TextRemoveEvent event = (TextRemoveEvent) obj;
			if(event.getOffset() == getOffset() && event.getLength() == getLength())
				return true;
			else
				return false;
		}
		else
			return false;
	}
}
