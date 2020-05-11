package main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;

import main.MerkleTree.Node;

public class Block implements Serializable {
	Header header;
	String headerHash;
	MerkleTree merkleTree;
	int targetThreshold;

	class Header implements Serializable {
		private static final long serialVersionUID = 4851452482770345670L;
		String pHeaderHash;
		Date timestamp;
		int Nonce;
		Node root;
	}

	public Block(MerkleTree merkleTree, String pHeaderHash) {
		header = new Header();
		this.merkleTree = merkleTree;
		header.root = merkleTree.root;
		header.pHeaderHash = pHeaderHash;
		header.Nonce = 0;
		targetThreshold = 1;
		while (true) {
			try {
				header.timestamp = new Timestamp(System.currentTimeMillis());

				String seriazedHeader = toString(header);
				String hash = sha256(seriazedHeader);

				int countZero = 0;
				boolean target = false;
				for (int i = 0; i < hash.length(); i++) {
					if (hash.charAt(i) == '0') {
						countZero++;
						if (countZero >= targetThreshold) {
							target = true;
							break;
						}
					} else {
						break;
					}
				}
				if (target) {
					headerHash = hash;
					break;
				} else {
					header.Nonce = header.Nonce + 1;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String toString(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	public static String sha256(String base) {

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
