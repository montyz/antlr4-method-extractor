package z;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

public class MethodFinder extends Java7BaseListener {
	String[] methodNamesToMatch;
	File currentJavaFile;
	File outputDir;
	List<Interval> intervals;
	CharStream input;

	public MethodFinder(String[] methodNames, String outputDir) { 
		this.methodNamesToMatch = methodNames;
		this.outputDir = new File(outputDir);
		this.outputDir.mkdirs();
	}

	@Override public void enterMethodDeclaration(Java7Parser.MethodDeclarationContext ctx) {
		TerminalNode identifier = ctx.Identifier();
		if (identifier!=null) {
			for (String name:methodNamesToMatch) {
				if (identifier.getText().equals(name)) {
					int a = ctx.start.getStartIndex();
					int b = ctx.stop.getStopIndex();
					Interval interval = new Interval(a,b);
					intervals.add(interval);
				}
			}
		}
	}

	public void walkDirectory( File dir ) {
		for( File child : dir.listFiles() ) {
			if( child.isDirectory() ) {
				walkDirectory( child );
			} else {
				if (child.getName().endsWith(".java")) {
					parseFile(child);
				}
			}
		}
	}

	private void parseFile(File child) {
		try {
			currentJavaFile = child;
			intervals = new ArrayList<Interval>();
			input = new ANTLRFileStream(child.getPath());
			Java7Lexer lexer = new Java7Lexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			Java7Parser parser = new Java7Parser(tokens);
			ParserRuleContext tree = parser.compilationUnit();
			ParseTreeWalker walker = new ParseTreeWalker();
			walker.walk(this, tree);
			if (intervals.size()>0) {
				writeOutputFile();
			}
		} catch (IOException e) {
			System.err.println("Could not parse " + child.getPath());
			e.printStackTrace();
		}	
	}

	private void writeOutputFile() {
		File outFile = new File(this.outputDir, this.currentJavaFile.getName());
		try {
			FileWriter fw = new FileWriter(outFile);
			for (Interval interval:this.intervals) {
				fw.write(input.getText(interval));
				fw.write("\n\n");
			}
			fw.close();
		} catch (Exception e) {
			System.err.println("Could not write output file " + outFile);
			e.printStackTrace();
		}
	}


	public static void main(String[] args) throws IOException {
		if (args.length<=2) {
			System.err.println("usage: MethodFinder javaDirectoryToWalk outputDir methodName1 [methodName2 methodName3 ...]");
		}
		MethodFinder extractor = new MethodFinder(Arrays.copyOfRange(args, 2, args.length), args[1]);
		extractor.walkDirectory(new File(args[0]));
	}
}