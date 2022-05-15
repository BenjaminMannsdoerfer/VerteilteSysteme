package aqua.blatt1.broker;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final T client;
		final Timestamp timeStamp;

		Client(String id, T client, Timestamp timeStamp) {
			this.id = id;
			this.client = client;
			this.timeStamp = timeStamp;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client, Timestamp timeStamp) {
		clients.add(new Client(id, client, timeStamp));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		return clients.get(index).client;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

	public void setTimeStamp(int index, long timestamp) {
		/*Client oldClient = clients.get(index);
		Client newClient = new Client(oldClient.id, oldClient.client, timestamp);
		clients.set(index, newClient);*/
		clients.get(index).timeStamp.setTime(timestamp);
		//clients.get(index).timeStamp.setTime(timestamp);
	}

	public Long getTimeStamp(int index) {
		return clients.get(index).timeStamp.getTime();
	}

	public String getId(int index) {
		return clients.get(index).id;
	}



}
