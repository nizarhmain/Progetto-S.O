import java.io.FileInputStream;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class OldPolish {

	static final int numberOfThreads = Runtime.getRuntime().availableProcessors(); // a more generic version
	// single threaded was actually faster in short expressions, due to the overhead created by the Callable interface 

	// we first start by converting bytes to charsets
	// the buffer containing bytes => characters
	// charset used for decoding
	public static String bb_to_str(ByteBuffer buffer, Charset charset) {

		byte[] bytes;
		if (buffer.hasArray()) {
			bytes = buffer.array();
		} else {
			bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
		}
		return new String(bytes, charset);
	}

	public static void main(String[] args) throws IOException {
		Instant start = Instant.now();

		FileInputStream fIn;
		FileChannel fChan;
		long fSize;

		try {
			fIn = new FileInputStream("input.txt");
			fChan = fIn.getChannel();
			fSize = fChan.size();

			// !!! important if i remove this line, the whole execution is slowed
			// pre allocating and instanciating 2 million chars, even thought
			// we don't technically use them but the program runs considerably quicker
			// feels like the memory allocate for the chars is somehow used by the buffer 
			// I have no idea why this works
			// this is no longer revelant, since i won't use trees that have more than 32000 nodes
			char[] chars = new char[(int) fSize];

			ByteBuffer mBuf = ByteBuffer.allocateDirect((int) fSize);

			fChan.read(mBuf);

			mBuf.rewind();
			// after many tries, this approach is quicker
			// from 22 Million characters ( including spaces ) : 90ms
			// to Forming a string from the bytebuffer and return around 2 Million Strings
			// 247ms for the split, was 896 ms for the other method
			String[] enzo = bb_to_str(mBuf, Charset.forName("UTF-8")).split(" ");
			//
			// if we allocate Two million array holders here, it doesn't speed up the thing
			//System.out.println(bb_to_str(mBuf, Charset.forName("UTF-8")).length());
			System.out.println("L'albero a " + enzo.length + " nodi");
			System.out.println("CORES : " + numberOfThreads);

			// asynchronous call is being executed here
			ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
			System.out.println("...Reading");

			long currentTimeMillis = System.currentTimeMillis();

			ParseTree tree = ParseTree.fromPrefix(enzo);

			// stampa l'espressione polacca
			// System.out.println(tree.toPrefix());

			//stampa il sottoalbero destro 
			//System.out.println(tree.leftSubTree(tree.getRootNode()));
			// System.out.println(tree.rightSubTree(tree.getRootNode()));

			if (enzo.length >= 8) {
				TreeSubdivider(tree);
			} else {
				// directly evaluate if the expression has less than 8 nodes 
				System.out.println(ParseTree.evaluateNumbers(Double.parseDouble(tree.getRootNode().left.toString()),
						tree.getRootNode().toString(), Double.parseDouble(tree.getRootNode().right.toString())));

			}

			//instead of using this method, if the tree has less than 8 nodes, don't do this method but calculate directly

			System.out.println(System.currentTimeMillis() - currentTimeMillis + " ms");

			long currentTimeMillis2 = System.currentTimeMillis();

			// now we submit the calculation to the executor
			System.out.println("Submitting tasks for execution:");
			List<Future<Double>> results = new LinkedList<>();

			//System.out.println(rightTrees.get(0).toPrefix());

			// we might have to inverse the computation on the list,
			// so smaller trees start calculating first going towards the bigger tree and not the opposite
			// since this is a submit the computing results are in order 
			// not in the evaluation itself, but in the executor submit

			for (ParseTree Ltree : leftTrees) {
				results.add(executor.submit(Ltree));
			}

			for (ParseTree Rtree : rightTrees) {
				results.add(executor.submit(Rtree));
			}

			List<Double> finalCalculation = new LinkedList<>();

			System.out.println("Getting results from futures:");
			for (Future<Double> result : results) {
				try {
					System.out.println("computed result : " + result.get());
					finalCalculation.add(result.get());
				} catch (InterruptedException e) {
					System.out.println("Interrupted while waiting for result: " + e.getMessage());
				} catch (ExecutionException e) {
					System.out.println("A task ended up with an exception: " + e.getCause());
				}
			}

			System.out.println("length of the final caculation is : " + finalCalculation.size());

			// the results are from the most left branch of the tree to the most right branch of the tree
			// need to add more final calculations for the tree 
			// couldn't really optimize, the problem is that the sequential reading of the file
			// cannot be done in parallel, therefore the multithreading part of the program can only be applied on
			// the calculation which itself isn't really the bottleneck of the program, the bottleneck is the creation
			// of the tree itself.
			//
			//Double first = finalCalculation.get(0) + finalCalculation.get(1);
			//System.out.println("this the first result :  " + first);

			int firstNumberCounter = 0;
			int secondNumberCounter = 1;
			int operationCounter = finalCalculation.size() / 2 - 1;

			double[] firstResults = new double[4];

			//TODO the number of iterations needed is the length of the calculation / 2 

			// first line loop, first row of results

			int iterationToMake = finalCalculation.size() / 2;

			// TODO the operation that we get from the text enzo[x] depending on how long is the calculation
			// we have to make that generic 
			// the operationCounter is basically the Size/2 - 1 

			for (int i = 0; i < iterationToMake; i++) {
				System.out.println(i + 1 + " First round iteration --- result is : ");
				System.out.println(ParseTree.evaluateNumbers(finalCalculation.get(firstNumberCounter),
						enzo[operationCounter], finalCalculation.get(secondNumberCounter)));
				firstResults[i] = ParseTree.evaluateNumbers(finalCalculation.get(firstNumberCounter),
						enzo[operationCounter], finalCalculation.get(secondNumberCounter));
				firstNumberCounter = firstNumberCounter + 2;
				secondNumberCounter = secondNumberCounter + 2;
				operationCounter++;
			}

			// second line loop, second row of results
			// the second loop round is only need when the length of the total calculation equals 8
			// the tree will always be subdivided in subtrees one for each core
			if (finalCalculation.size() == numberOfThreads) {
				double[] secondResults = new double[2];

				int cc1 = 0;

				for (int i = 0; i < 2; i++) {
					System.out.println(i + 1 + " Second round iteration --- results is : ");
					System.out
							.println(ParseTree.evaluateNumbers(firstResults[cc1], enzo[i + 1], firstResults[cc1 + 1]));
					secondResults[i] = ParseTree.evaluateNumbers(firstResults[cc1], enzo[i + 1], firstResults[cc1 + 1]);
					cc1 = cc1 + 2;
				}

				System.out.println(
						"FINAL RESULT IS : " + ParseTree.evaluateNumbers(secondResults[0], enzo[0], secondResults[1]));

			}

			System.out.println("Shutting down the executor.");
			executor.shutdown();

			//tree.evaluate();	       
			System.out.println(System.currentTimeMillis() - currentTimeMillis2 + " ms computation time");

			//tree.printTree();

			//tree.printLeftTree();
			//tree.printRightTree();

			fChan.close();
			fIn.close();

		} catch (IOException exc) {
			System.out.println(exc);
			System.exit(1);
		}

		long seconds = Duration.between(start, Instant.now()).toMillis();
		System.out.println("the operation took " + seconds + " milliseconds");

	}

	static int counterLeft = 0;
	static int counterRight = 0;
	static List<ParseTree> leftTrees = new LinkedList<>();
	static List<ParseTree> rightTrees = new LinkedList<>();

	public static void TreeSubdivider(ParseTree tree) {

		tree.printTree();
		//if the tree has less than 8 nodes, we do not wish to do this method but instead
		// calculate directly

		// avoid making left and right trees if they're are only two branches of the main tree
		// basically small operations	
		if (counterLeft < numberOfThreads / 2 && tree.leftSubTreeTokenized().length / 2 > 0) {
			System.out.println(tree.leftSubTreeTokenized().length);

			ParseTree leftTree = ParseTree.fromPrefix(tree.leftSubTreeTokenized());
			counterLeft++;
			System.out.println("we can still create Left-trees : " + counterLeft);
			//System.out.println(leftTree.toPrefix());			
			leftTree.printTree();

			leftTrees.add(leftTree);
			System.out.println("the size is : " + leftTrees.size() + " contains : ");
			TreeSubdivider(leftTree);

		}

		if (counterRight < numberOfThreads / 2 && tree.rightSubTreeTokenized().length / 2 > 0) {
			System.out.println(tree.rightSubTreeTokenized().length);

			ParseTree rightTree = ParseTree.fromPrefix(tree.rightSubTreeTokenized());
			counterRight++;
			//System.out.println(rightTree.toPrefix());
			System.out.println("we can still create Right-trees : " + counterRight);
			rightTree.printTree();

			rightTrees.add(rightTree);
			System.out.println("the size is : " + rightTrees.size() + " contains : ");
			TreeSubdivider(rightTree);

		}

	}

}
