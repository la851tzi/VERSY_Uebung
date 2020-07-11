package aqua.blatt1;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {

    private final String keyMaterial = "CAFEBABECAFEBABE";
    private final String encryptionAlgorithm = "AES";
    private final DatagramSocket socket;
    private Endpoint endpoint;
    private SecretKeySpec key;
    private Cipher encrypt;
    private Cipher decrypt;

    public SecureEndpoint() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, SocketException {
        this.socket = new DatagramSocket();
        this.endpoint = new Endpoint();
        this.key = new SecretKeySpec(keyMaterial.getBytes(), "AES");
        this.encrypt = Cipher.getInstance(encryptionAlgorithm);
        this.decrypt = Cipher.getInstance(encryptionAlgorithm);
        encrypt.init(Cipher.ENCRYPT_MODE, key);
        decrypt.init(Cipher.DECRYPT_MODE, key);
    }

    public SecureEndpoint(int port) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, SocketException {
        this.socket = new DatagramSocket(port);
        //this.endpoint = new Endpoint(port);
        this.key = new SecretKeySpec(keyMaterial.getBytes(), "AES");
        this.encrypt = Cipher.getInstance(encryptionAlgorithm);
        this.decrypt = Cipher.getInstance(encryptionAlgorithm);
        encrypt.init(Cipher.ENCRYPT_MODE, key);
        decrypt.init(Cipher.DECRYPT_MODE, key);
    }

    @Override
    public void send(InetSocketAddress address, Serializable payload){
        try {
            SealedObject s = new SealedObject(payload, encrypt);
            //endpoint.send(address,decrypt.doFinal((byte[]) payload));
            endpoint.send(address,s);
        } catch (IllegalBlockSizeException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message blockingReceive() {
        Message crypto = endpoint.blockingReceive();
        return decrypt(crypto);
    }

    @Override
    public Message nonBlockingReceive() {
        Message crypto = endpoint.nonBlockingReceive();
        return decrypt(crypto);
    }

    private Message decrypt(Message crypto){
        SealedObject cryptoPayload = (SealedObject) crypto.getPayload();
        try {
            Serializable serializable = (Serializable) cryptoPayload.getObject(decrypt);

            return new Message(serializable,crypto.getSender());
        } catch (IllegalBlockSizeException | ClassNotFoundException | IOException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
