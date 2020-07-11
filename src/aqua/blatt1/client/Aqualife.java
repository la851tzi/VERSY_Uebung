package aqua.blatt1.client;

import javax.crypto.NoSuchPaddingException;
import javax.swing.SwingUtilities;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Aqualife {

	public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, SocketException {
		ClientCommunicator communicator = new ClientCommunicator();
		TankModel tankModel = new TankModel(communicator.newClientForwarder());

		communicator.newClientReceiver(tankModel).start();

		SwingUtilities.invokeLater(new AquaGui(tankModel));

		tankModel.run();
	}
}
