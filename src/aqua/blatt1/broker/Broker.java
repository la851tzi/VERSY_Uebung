package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private Endpoint endpoint;
    private ClientCollection clientCollection;
    private volatile boolean stopRequested;
    private ReadWriteLock lock;
    private int counter;

    public Broker() {
        this.endpoint = new Endpoint(Properties.PORT);
        this.clientCollection = new ClientCollection();
        this.stopRequested = false;
        this.lock = new ReentrantReadWriteLock();
        this.counter = 0;
    }

    public class BrokerTask implements Runnable{

        private Message msg;

        public BrokerTask(Message message) {
            this.msg = message;
        }

        @Override
        public void run() {
            if (msg.getPayload() instanceof RegisterRequest) {
                InetSocketAddress sender = msg.getSender();
                RegisterResponse registerResponse = register(sender);
                endpoint.send(sender, registerResponse);
            }

            if (msg.getPayload() instanceof DeregisterRequest) {
                InetSocketAddress client = msg.getSender();
                deregister(client);
            }

            /* Changed in Übungsblatt 3 Aufgabe 1
            if (msg.getPayload() instanceof  HandoffRequest) {
                Direction fishDirection = ((HandoffRequest) msg.getPayload()).getFish().getDirection();
                InetSocketAddress sender = msg.getSender();
                InetSocketAddress receiver = handoffFish(fishDirection, sender);
                endpoint.send(receiver, msg.getPayload());
            }
            */

            if (msg.getPayload() instanceof PoisonPill) {
                System.exit(0);
            }

            if (msg.getPayload() instanceof NameResolutionRequest) {
                sendNameResolutionResponse(((NameResolutionRequest) msg.getPayload()).getTankId(), ((NameResolutionRequest) msg.getPayload()).getRequestId(), msg.getSender());
            }
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    public void broker() {
        ExecutorService executor = Executors.newFixedThreadPool(Properties.NUMBER_THREADS);
        /* Übungsblatt 2 Aufgabe 2
        executor.execute(() -> {
            JOptionPane.showMessageDialog(null, "Press OK button to poison server.", "stopRequested", JOptionPane.OK_OPTION);
            stopRequested = true;
        });
        */
        while (!stopRequested) {
            Message msg = endpoint.blockingReceive();
            executor.execute(new BrokerTask(msg));
        }
        executor.shutdown();
    }

    private RegisterResponse register(InetSocketAddress newClient) {
        lock.writeLock().lock();
        String newClientId = getClientIdForRegistering();
        clientCollection.add(newClientId, newClient);

        int newClientIndex = clientCollection.indexOf(newClient);
        InetSocketAddress leftNeighbor = (InetSocketAddress) clientCollection.getLeftNeighorOf(newClientIndex);
        InetSocketAddress rightNeighbor = (InetSocketAddress) clientCollection.getRightNeighorOf(newClientIndex);
        NeighborUpdate newNeighborUpdate = new NeighborUpdate(leftNeighbor, rightNeighbor);
        endpoint.send(newClient, newNeighborUpdate);

        sendRightNeighborNeighborUpdate(newClient, rightNeighbor);
        sendLeftNeighborNeighborUpdate(newClient, leftNeighbor);

        if(clientCollection.size() == 1) {
            endpoint.send(newClient, new Token());
        }

        lock.writeLock().unlock();
        return new RegisterResponse(newClientId);
    }

    private String getClientIdForRegistering() {
        String newClientName = "tank" + counter;
        counter++;
        return newClientName;
    }

    private void deregister(InetSocketAddress clientToBeRemoved) {
        lock.writeLock().lock();
        int clientToBeRemovedIndex = clientCollection.indexOf(clientToBeRemoved);
        InetSocketAddress leftNeighbor = (InetSocketAddress) clientCollection.getLeftNeighorOf(clientToBeRemovedIndex);
        InetSocketAddress rightNeighbor = (InetSocketAddress) clientCollection.getRightNeighorOf(clientToBeRemovedIndex);
        sendLeftNeighborNeighborUpdate(rightNeighbor, leftNeighbor);
        sendRightNeighborNeighborUpdate(leftNeighbor, rightNeighbor);
        clientCollection.remove(clientToBeRemovedIndex);
        lock.writeLock().unlock();
    }

    /* Changed in Übungsblatt 3 Aufgabe 1
    private InetSocketAddress handoffFish(Direction fishDirection, InetSocketAddress sender) {
        lock.readLock().lock();
        int senderIndex = clientCollection.indexOf(sender);
        if (fishDirection.equals(Direction.LEFT)) {
            lock.readLock().unlock();
            return (InetSocketAddress) clientCollection.getLeftNeighorOf(senderIndex);
        }
        lock.readLock().unlock();
        return (InetSocketAddress) clientCollection.getRightNeighorOf(senderIndex);
    }
     */

    private void sendLeftNeighborNeighborUpdate(InetSocketAddress newRightNeighbor, InetSocketAddress leftNeighbor) {
        int leftNeighborIndex = clientCollection.indexOf(leftNeighbor);
        InetSocketAddress leftFromLeftNeighbor = (InetSocketAddress) clientCollection.getLeftNeighorOf(leftNeighborIndex);
        NeighborUpdate leftNeighborUpdate = new NeighborUpdate(leftFromLeftNeighbor, newRightNeighbor);
        endpoint.send(leftNeighbor, leftNeighborUpdate);
    }

    private void sendRightNeighborNeighborUpdate(InetSocketAddress newLeftNeighbor, InetSocketAddress rightNeighbor) {
        int rightNeighborIndex = clientCollection.indexOf(rightNeighbor);
        InetSocketAddress rightFromRightNeighbor = (InetSocketAddress) clientCollection.getRightNeighorOf(rightNeighborIndex);
        NeighborUpdate rightNeighborUpdate = new NeighborUpdate(newLeftNeighbor, rightFromRightNeighbor);
        endpoint.send(rightNeighbor, rightNeighborUpdate);
    }

    private void sendNameResolutionResponse(String tankId, String requestId, InetSocketAddress sender) {
        InetSocketAddress addressTankId = (InetSocketAddress) clientCollection.getClient(clientCollection.indexOf(tankId));
        NameResolutionResponse nameResolutionResponse = new NameResolutionResponse(addressTankId, requestId, sender);
        endpoint.send(sender, nameResolutionResponse);
    }
}
