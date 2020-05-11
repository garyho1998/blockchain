package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import main.MerkleTree.Node;

public class mainProgram {

	public static void main(String[] args) throws InterruptedException, CloneNotSupportedException, IOException {
		// TODO Auto-generated method stub

		List<String> transactions = new ArrayList<String>();
		String[] actions = { "Alice -> Bob: 10", "Alice -> Bob: 1", "Charlie -> Dan: 6", "Dan -> Bob: 2",
				"Bob -> Alice: 4", "Elle -> Alice: 9", "Bob -> Alice: 5", "Elle -> Alice: 3" };

		List<Block> blockChain = makeBlockChainTest(transactions, actions);

		String transaction3a = transactions.get(4);
		String transaction4b = transactions.get(7);
		verifyTransactionTest("3a", transaction3a, blockChain);
		System.out.println(" ");
		verifyTransactionTest("4b", transaction4b, blockChain);

	}
	
	//Create BlockChain
	static List<Block> makeBlockChainTest(List<String> transactions, String[] actions)
			throws CloneNotSupportedException, IOException {
		int blockCount = 0;
		List<String> tempTransactions = new ArrayList<String>();
		List<Block> blockChain = new ArrayList<Block>();

		for (int i = 0; i < 1; i++) {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			tempTransactions.add("[" + timestamp + "] " + actions[i]);
			transactions.add("[" + timestamp + "] " + actions[i]);
		}
		blockCount = addBlock(blockChain, blockCount, tempTransactions);

		for (int i = 1; i < 4; i++) {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			tempTransactions.add("[" + timestamp + "] " + actions[i]);
			transactions.add("[" + timestamp + "] " + actions[i]);
		}
		blockCount = addBlock(blockChain, blockCount, tempTransactions);

		for (int i = 4; i < 6; i++) {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			tempTransactions.add("[" + timestamp + "] " + actions[i]);
			transactions.add("[" + timestamp + "] " + actions[i]);
		}
		blockCount = addBlock(blockChain, blockCount, tempTransactions);

		for (int i = 6; i < 8; i++) {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			tempTransactions.add("[" + timestamp + "] " + actions[i]);
			transactions.add("[" + timestamp + "] " + actions[i]);
		}
		blockCount = addBlock(blockChain, blockCount, tempTransactions);

		return blockChain;
	}

	static int addBlock(List<Block> blockChain, int blockCount, List<String> tempTransactions)
			throws CloneNotSupportedException, IOException {
		if (blockCount == 0) {
			MerkleTree merkleTree = new MerkleTree(tempTransactions);
			blockChain.add(new Block(merkleTree, "initHash"));
		} else {
			MerkleTree newTree = (MerkleTree) blockChain.get(blockCount - 1).merkleTree.clone();
			// Record all transaction since the start of blockChain creation. It can be
			// changed by create a new tree instead
			newTree.addTransactions(tempTransactions);
			blockChain.add(new Block(newTree, blockChain.get(blockCount - 1).headerHash));
		}

		try (FileWriter fw = new FileWriter("blockChain.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println("Chain" + blockCount + ": ");
			printBlockChain(blockChain, out);
			out.println("");
			out.println("");
			out.println("");
		} catch (IOException e) {

		}
		tempTransactions.clear();
		return ++blockCount;
	}

	//Verify Transactions test
	static void verifyTransactionTest(String tag, String transaction, List<Block> blockChain)
			throws CloneNotSupportedException {
		List<Node> proof = makeMerkleProof(transaction, blockChain);
		if (proof == null) {
			System.out.println(tag + " is not in the blockchain");
		}
		{
			System.out.println(tag + "  is in the blockchain");
			printProof(proof);

			Node mTreeRoot = blockChain.get(blockChain.size() - 1).merkleTree.root;
			if (verifyTransactionByProve(transaction, proof, mTreeRoot)) {
				System.out.println(tag + " Verified");
			} else {
				System.out.println(tag + " Not Verified");
			}

		}
	}

	static Boolean verifyTransactionByProve(String transaction, List<Node> proof, Node root) {
		String hash = ""; // if transaction is on the right of its neighbor

		if (proof.get(proof.size() - 1).OnLeft) {
			hash = sha256(proof.get(proof.size() - 1).value, sha256(transaction, ""));
		} else {
			hash = sha256(sha256(transaction, ""), proof.get(proof.size() - 1).value);
		}
		for (int i = proof.size() - 2; i >= 0; i--) {
			if (proof.get(i).OnLeft) {
				hash = sha256(proof.get(i).value, hash);
			} else {
				hash = sha256(hash, proof.get(i).value);
			}
		}

		if (root.value.contentEquals(hash)) {
			return true;
		}
		return false;
	}
	

	static List<Node> makeMerkleProof(String transaction, List<Block> blockChain) throws CloneNotSupportedException {
		Block lastBlock = blockChain.get(blockChain.size() - 1);
		MerkleTree mTree = lastBlock.merkleTree;

		List<Node> route = new ArrayList<Node>();
		Boolean found = mTree.findTransaction(mTree.root, route, transaction);
		List<Node> proof = null;

		if (found) {
			printRoute(route);
			mTree.displayValues();
			proof = mTree.returnMerkleProof(route);
		}

		return proof;
	}

	// print section
	static void printBlockChain(List<Block> blockChain, PrintWriter out) throws IOException {
		for (int i = 0; i < blockChain.size(); i++) {
			out.println("Block(" + i + "):");
			blockChain.get(i).merkleTree.outputValues(out);
			if (i != blockChain.size() - 1) {
				out.println("|");
				out.println("V");
			}

		}
	};

	static void printRoute(List<Node> route) {
		System.out.print("Route: ");
		for (int i = 0; i < route.size() - 1; i++) {
			System.out.print(route.get(i).id + "->");
		}
		System.out.println(route.get(route.size() - 1).id);
	}

	static void printProof(List<Node> proof) {
		System.out.print("Proof: ");
		for (int i = 0; i < proof.size() - 1; i++) {
			System.out.print(proof.get(i).id + "(OnLeft:" + proof.get(i).OnLeft + "): " + proof.get(i).value + " -> ");

		}
		System.out.println(proof.get(proof.size() - 1).id + "(OnLeft:" + proof.get(proof.size() - 1).OnLeft + "): "
				+ proof.get(proof.size() - 1).value);
	}

	public static String sha256(String a, String b) {
		String base = a + b;
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
