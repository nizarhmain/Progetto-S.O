
public class Node {
	public final String key;
	public Node left;
	public Node right;

	// Used to indicate whether the right pointer is a normal
	// right pointer or a pointer to inorder successor.
	public Node(String key) {
		this.key = key;
		left = right = null;
	}

	@Override
	public String toString() {
		return key;
	}

}
