
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.Callable;

public class ParseTree implements Callable<Double> {
	// difference between Callable and runnable is that the callable actually returns a result

	private static final String[] operations = { "+", "-", "*", "/" };
	private Node root;
	private static int nth = 0;
	private final int id = nth++;

	public ParseTree() {
		//default constructor, doesn't really do anything
	}

	//computes a result or throws an exception if can't
	@Override
	public Double call() throws Exception {
		Double value = evaluate(root);

		System.out.printf("Task %d started computing...%n", id);

		System.out.printf("Task %d started computing...%n", id);
		System.out.printf("Task %d is returning value: %f%n", id, value);

		return value;

		// return evaluate(root);     
	}

	/** factory methods 
	 * @return */
	// we passed the splitted tokens, that we created before into this method, tokens => keys in this context
	// we not only create the tree from a string of keys, but, if we see that there are two consecutive numbers and
	// an operation behind them, we immediately do the calculation, and reduce the size of tree considerably
	// this is a single threaded operation that makes the ParseTree "shorter" with less branches to feed in to 
	// the multithreaded part of the program that splits the tree in different "Mini trees" and executes a call on them
	// depending on how many cores there are
	public static ParseTree fromPrefix(String[] keys) {
		ParseTree tree = new ParseTree();
		for (int i = 0; i < keys.length; i++) {
			if (isOperation(keys[i])) {

				if (isOperation(keys[i + 1])) {

					tree.addLeft(keys[i]);

				} else {

					tree.addLeft(String.valueOf(
							evaluateNumbers(Double.valueOf(keys[i + 1]), keys[i], Double.valueOf(keys[i + 2]))));
				}
			}
		}
		return tree;
	}

	/*
	public static void getRoot(){
		System.out.println(" the root is : " + root.key);
		System.out.println(" the left child :" + root.left.key);
		System.out.println(" the right child : " + root.right.key);
	}
	*/

	public Node getRootNode() {
		return root;
	}

	/** returns true iff key represents is a valid operation */
	public static boolean isOperation(String key) {
		for (String operation : operations)
			if (operation.equals(key)) {

				return true;
			}
		return false;
	}

	/** evaluates operands by the operations specified in a given string */
	public static double evaluateNumbers(double lhs, String operation, double rhs) {

		switch (operation) {
		case "+":
			return lhs + rhs;
		case "-":
			return lhs - rhs;
		case "*":
			return lhs * rhs;
		case "/":
			return lhs / rhs;
		}

		return 0; //this should never happen
	}

	/* returns tokenized string ready for the parsing*/
	public String[] leftSubTreeTokenized() {
		String x = leftSubTree(root);
		return x.split(" ");
	}

	public String[] rightSubTreeTokenized() {
		String x = rightSubTree(root);
		return x.split(" ");
	}

	/** print entire tree as prefix */
	public String toPrefix() {
		return toPrefix(root);
	}

	public String leftSubTree(Node node) {
		return toPrefix(node.left);
	}

	public String rightSubTree(Node node) {
		return toPrefix(node.right);
	}

	private String toPrefix(Node node) {
		if (node.left == null)
			return node.key;
		return node.key + " " + toPrefix(node.left) + " " + toPrefix(node.right);
	}

	/** evaluate and return the value of the entire tree */
	public void evaluate() {
		System.out.println(evaluate(root));
	}

	private double evaluate(Node node) {

		if (isOperation(node.key)) {

			return evaluateNumbers(evaluate(node.left), node.key, evaluate(node.right));

		} else {
			return Double.valueOf(node.key);
		}

	}

	/** adds a new node to the leftmost possible position without violating the tree's properties */
	public void addLeft(String key) {
		if (root == null)
			root = new Node(key);
		else
			addLeftRecursive(root, key);
	}

	private boolean addLeftRecursive(Node node, String key) {
		if (!isOperation(node.key))
			return false;

		if (node.left == null) {
			node.left = new Node(key);
			return true;
		}

		if (addLeftRecursive(node.left, key))
			return true;

		if (node.right == null) {
			node.right = new Node(key);
			return true;
		}
		return addLeftRecursive(node.right, key);
	}

	public void printTree() {
		printSubtree(root);
	}

	public void printLeftTree() {
		printSubtree(root.left);
	}

	public void printRightTree() {
		printSubtree(root.right);
	}

	public void printSubtree(Node node) {
		if (node.right != null) {
			printTree(node.right, true, "");
		}
		printNodeValue(node);
		if (node.left != null) {
			printTree(node.left, false, "");
		}
	}

	private void printTree(Node node, boolean isRight, String indent) {
		if (node.right != null) {
			printTree(node.right, true, indent + (isRight ? "        " : " |      "));
		}
		System.out.print(indent);
		if (isRight) {
			System.out.print(" /");
		} else {
			System.out.print(" \\");
		}
		System.out.print("----- ");
		printNodeValue(node);
		if (node.left != null) {
			printTree(node.left, false, indent + (isRight ? " |      " : "        "));
		}
	}

	private void printNodeValue(Node node) {
		if (node.key == null) {
			System.out.print("<null>");
		} else {
			System.out.print(node.key.toString());
		}
		System.out.println();
	}

}
