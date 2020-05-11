package main;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MerkleTree implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	List<String> transactions;
	Node root;
	int depth;
	int num_nodes;
	int node_id_count;
	
	static class Node implements Serializable, Cloneable {
		private static final long serialVersionUID = 1L;
		public String type; // INTERNAL or LEAF
		public String value;
		public String hash;
		public Node left;
		public Node right;
		public String id;
		public Boolean OnLeft;

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public MerkleTree(List<String> transactions) {
		constructTree(transactions);
	}

	void constructTree(List<String> transactions) {
		if (transactions.size() == 0) {
			throw new IllegalArgumentException("No transactions");
		}

		this.transactions = new ArrayList<String>(transactions);
		num_nodes = transactions.size();
		depth = 1;
		node_id_count = 0;

		if (transactions.size() == 1) {
			Node leaf = constructLeafNode(transactions.get(0));
			root = constructInternalNode(leaf, null);
		} else {
			List<Node> parents = bottomLevel(transactions);
			num_nodes += parents.size();

			while (parents.size() > 1) {
				parents = internalLevel(parents);
				depth++;
				num_nodes += parents.size();
			}
			root = parents.get(0);
		}

	}

	MerkleTree addTransactions(List<String> newTransactions) {
		List<String> newList = Stream.concat(this.transactions.stream(), newTransactions.stream())
				.collect(Collectors.toList());
		constructTree(newList);

		return this;
	}

	List<Node> bottomLevel(List<String> transactions) {
		List<Node> parents = new ArrayList<Node>(transactions.size() / 2);

		for (int i = 0; i < transactions.size() - 1; i += 2) {
			Node leaf1 = constructLeafNode(transactions.get(i));
			Node leaf2 = constructLeafNode(transactions.get(i + 1));

			Node parent = constructInternalNode(leaf1, leaf2);
			parents.add(parent);
		}

		// if odd number of leafs, handle last entry
		if (transactions.size() % 2 != 0) {
			Node leaf = constructLeafNode(transactions.get(transactions.size() - 1));
			Node parent = constructInternalNode(leaf, null);
			parents.add(parent);
		}

		return parents;
	}

	List<Node> internalLevel(List<Node> children) {
		List<Node> parents = new ArrayList<Node>(children.size() / 2);

		for (int i = 0; i < children.size() - 1; i += 2) {
			Node child1 = children.get(i);
			Node child2 = children.get(i + 1);

			Node parent = constructInternalNode(child1, child2);
			parents.add(parent);
		}

		if (children.size() % 2 != 0) {
			Node child = children.get(children.size() - 1);
			Node parent = constructInternalNode(child, null);
			parents.add(parent);
		}

		return parents;
	}

	private Node constructInternalNode(Node child1, Node child2) {
		Node parent = new Node();
		parent.type = "INTERNAL";

		if (child2 == null) {
			parent.id = child1.id + "+null";
			if (child1.type == "LEAF") {
				parent.value = sha256(child1.hash, "");
			} else {
				parent.value = sha256(child1.value, "");
			}

		} else {
			parent.id = child1.id + "+" + child2.id;
			parent.value = sha256(child1.value, child2.value);
			if (child1.type == "LEAF") {
				parent.value = sha256(child1.hash, child2.hash);
			} else {
				parent.value = sha256(child1.value, child2.value);
			}
		}

		parent.left = child1;
		parent.right = child2;
		return parent;
	}

	private Node constructLeafNode(String value) {
		Node leaf = new Node();
		leaf.type = "LEAF";
		leaf.value = value;
		leaf.hash = sha256(value, "");
		leaf.id = Integer.toString(node_id_count);
		node_id_count++;
		return leaf;
	}

	void printPostorder(Node node) {
		if (node == null)
			return;

		// first recur on left subtree
		printPostorder(node.left);

		// then recur on right subtree
		printPostorder(node.right);

		// now deal with the node
		System.out.print(node.value + " ");
	}

	// it will construct route while finding the target
	Boolean findTransaction(Node node, List<Node> route, String target) {
		if (node == null)
			return false;

		route.add(node);
		// now deal with the node
		// System.out.println("NodeID: " + node.id + " ");
		if (node.value.contentEquals(target)) {
			// System.out.println("Found:" + target);
			return true;
		} else if (findTransaction(node.left, route, target) || findTransaction(node.right, route, target)) {
			return true;
		} else {
			route.remove(route.size() - 1);
			return false;
		}
	}

	List<Node> returnMerkleProof(List<Node> route) throws CloneNotSupportedException {
		List<Node> prove = new ArrayList<Node>();
		for (int i = 0; i < route.size() - 1; i++) {
			String nextNodeValue = route.get(i + 1).value;
			if (route.get(i).left.value.contentEquals(nextNodeValue)) {
				Node temp = (Node) route.get(i).right.clone();
				temp.OnLeft = false;
				prove.add(temp);
			} else {
				Node temp = (Node) route.get(i).left.clone();
				temp.OnLeft = true;
				prove.add(temp);
			}
		}

		Node last = (Node) prove.get(prove.size() - 1).clone();
		last.value = last.hash;
		prove.remove(prove.size() - 1);
		prove.add(last);
		return prove;
	}

	public void displayValues() {
		Stack<Node> globalStack = new Stack<Node>();
		globalStack.push(root);
		int emptyLeaf = 32;
		boolean isRowEmpty = false;
		System.out.println("{");
		while (isRowEmpty == false) {

			Stack<Node> localStack = new Stack<Node>();
			isRowEmpty = true;
			for (int j = 0; j < emptyLeaf; j++)
				System.out.print(" ");
			while (globalStack.isEmpty() == false) {
				Node temp = (Node) globalStack.pop();
				if (temp != null) {
					System.out.print(temp.id + ": " + temp.value);
					localStack.push(temp.left);
					localStack.push(temp.right);
					if (temp.left != null || temp.right != null)
						isRowEmpty = false;
				} else {
					System.out.print(" ");
					localStack.push(null);
					localStack.push(null);
				}
				for (int j = 0; j < emptyLeaf * 2 - 2; j++)
					System.out.print(" ");
			}
			System.out.println(" ");
			emptyLeaf /= 2;
			while (localStack.isEmpty() == false)
				globalStack.push(localStack.pop());
		}
		System.out.println("}");
	}

	public void outputValues(PrintWriter out) throws IOException {
		Stack<Node> globalStack = new Stack<Node>();
		globalStack.push(root);
		int emptyLeaf = 32;
		boolean isRowEmpty = false;
		out.println("{");
		while (isRowEmpty == false) {

			Stack<Node> localStack = new Stack<Node>();
			isRowEmpty = true;
			for (int j = 0; j < emptyLeaf; j++)
				out.print(" ");
			while (globalStack.isEmpty() == false) {
				Node temp = (Node) globalStack.pop();
				if (temp != null) {
					out.print(temp.value);
					localStack.push(temp.left);
					localStack.push(temp.right);
					if (temp.left != null || temp.right != null)
						isRowEmpty = false;
				} else {
					out.print("  ");
					localStack.push(null);
					localStack.push(null);
				}
				for (int j = 0; j < emptyLeaf * 2 - 2; j++)
					out.print("  ");
			}
			out.println("  ");
			emptyLeaf /= 2;
			while (localStack.isEmpty() == false)
				globalStack.push(localStack.pop());
		}
		out.println("}");
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
