package org.ruleml.psoa.psoatransrun;

import java.io.*;
import java.util.Scanner;

import org.ruleml.psoa.psoa2x.common.Translator;
import org.ruleml.psoa.psoa2x.psoa2prolog.PSOA2PrologConfig;
import org.ruleml.psoa.psoa2x.psoa2prolog.PrologTranslator;
import org.ruleml.psoa.psoa2x.psoa2tptp.PSOA2TPTPConfig;
import org.ruleml.psoa.psoa2x.psoa2tptp.TPTPTranslator;
import org.ruleml.psoa.psoatransrun.engine.ExecutionEngine;
import org.ruleml.psoa.psoatransrun.prolog.XSBEngine;
import org.ruleml.psoa.psoatransrun.test.TestSuite;
import org.ruleml.psoa.psoatransrun.tptp.VampirePrimeEngine;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import static org.ruleml.psoa.utils.IOUtil.*;

/**
 * Command line interface of PSOATransRun
 * 
 */
public class PSOATransRunCmdLine {
	// Entrance of PSOATransRun program
	public static void main(String[] args) throws IOException {
		// Parse command line options
		LongOpt[] opts = new LongOpt[] {
				new LongOpt("help", LongOpt.NO_ARGUMENT, null, '?'),
				new LongOpt("longhelp", LongOpt.NO_ARGUMENT, null, '~'),
				new LongOpt("targetLang", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
				new LongOpt("input", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
				new LongOpt("query", LongOpt.REQUIRED_ARGUMENT, null, 'q'),
				new LongOpt("test", LongOpt.NO_ARGUMENT, null, 't'),
				new LongOpt("numRuns", LongOpt.REQUIRED_ARGUMENT, null, 'n'),
				new LongOpt("echoInput", LongOpt.NO_ARGUMENT, null, 'e'),
				new LongOpt("printTrans", LongOpt.NO_ARGUMENT, null, 'p'),
				new LongOpt("outputTrans", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("xsbfolder", LongOpt.REQUIRED_ARGUMENT, null, 'x'),
				new LongOpt("allAns", LongOpt.NO_ARGUMENT, null, 'a'),
				new LongOpt("timeout", LongOpt.REQUIRED_ARGUMENT, null, 'm'),
				new LongOpt("staticOnly", LongOpt.NO_ARGUMENT, null, 's'),
				new LongOpt("undiff", LongOpt.NO_ARGUMENT, null, 'u'),
				new LongOpt("omitNegMem", LongOpt.NO_ARGUMENT, null, 'z')
		};

		Getopt optionsParser = new Getopt("", args, "?l:i:q:tn:epo:x:am:rsuz", opts);
		File kbFile = null;
		FileInputStream kbStream = null, queryStream = null;
		String arg;

		boolean outputTrans = false, showOrigKB = false, getAllAnswers = false, 
				dynamicObj = true, omitNegMem = false, differentiated = true,
				isTest = false;
		String inputPath = null, lang = null, transKBPath = null, xsbPath = null;
		int timeout = -1, numRuns = 1;
		
		for (int opt = optionsParser.getopt(); opt != -1; opt = optionsParser
				.getopt()) {
			switch (opt) {
			case '?':
				printUsage(false);
				return;
			case '~':
				printUsage(true);
				return;
			case 'l':
				lang = optionsParser.getOptarg();
				break;
			case 'i':
				inputPath = optionsParser.getOptarg();
				break;
			case 'q':
				arg = optionsParser.getOptarg();
				try {
					queryStream = new FileInputStream(arg);
				}
				catch (FileNotFoundException e) {
					printErrln("Cannot find query file ", arg,
							". Read from console.");
				}
				catch (SecurityException e) {
					printErrln("Unable to read query file ", arg,
							". Read from console.");
				}
				break;
			case 'm':
				try {
					timeout = Integer.parseInt(optionsParser.getOptarg());
				}
				catch (NumberFormatException e) {
					printErrlnAndExit("Incorrect number format for timeout");
				}
				break;
			case 't':
				isTest = true;
				break;
			case 'n':
				numRuns = 1;
				break;
			case 'e':
				showOrigKB = true;
				break;
			case 'p':
				outputTrans = true;
				break;
			case 'o':
				transKBPath = optionsParser.getOptarg();
				break;
			case 'x':
				xsbPath = optionsParser.getOptarg();
				break;
			case 'a':
				getAllAnswers = true;
				break;
			case 's':
				dynamicObj = false;
				break;
			case 'u':
				differentiated = false;
				break;
			case 'z':
				omitNegMem = true;
				break;
			default:
				assert false;
			}
		}

		// Try reading input file / directory
		try {
			kbFile = new File(inputPath);
			if (!isTest)
				kbStream = new FileInputStream(kbFile);
		}
		catch (NullPointerException e) {
			printErrlnAndExit("No input KB specified");
		}
		catch (FileNotFoundException e) {
			printErrlnAndExit("Cannot find KB file ", inputPath);
		}
		catch (SecurityException e) {
			printErrlnAndExit("Unable to read KB file ", inputPath);
		}

		// Initialize PSOATransRun
		ExecutionEngine engine = null;
		Translator translator = null;
		
		try {
			if (lang == null || lang.equalsIgnoreCase("prolog"))
			{
				PSOA2PrologConfig transConfig = new PSOA2PrologConfig();
				transConfig.dynamicObj = dynamicObj;
				transConfig.omitMemtermInNegativeAtoms = omitNegMem;
				transConfig.differentiateObj = differentiated;
				translator = new PrologTranslator(transConfig);
				
				XSBEngine.Config engineConfig = new XSBEngine.Config();
				engineConfig.transKBPath = transKBPath;
				engineConfig.xsbFolderPath = xsbPath;
				engine = new XSBEngine(engineConfig);
				
				if (timeout > 0)
					printErrln("Ignore -t option: only applicable for the target language TPTP");
			}
			else if (lang.equalsIgnoreCase("tptp"))
			{
				PSOA2TPTPConfig transConfig = new PSOA2TPTPConfig();
				transConfig.dynamicObj = dynamicObj;
				transConfig.omitMemtermInNegativeAtoms = omitNegMem;
				transConfig.differentiateObj = differentiated;
				translator = new TPTPTranslator(transConfig);
				VampirePrimeEngine.Config engineConfig = new VampirePrimeEngine.Config();
				if (timeout > 0)
					engineConfig.timeout = timeout;
				engineConfig.transKBPath = transKBPath;
				engine = new VampirePrimeEngine(engineConfig);
				
				if (xsbPath != null)
					printErrln("Ignore -x option: only applicable for the target language Prolog");
			}
			else
			{
				printErrlnAndExit("Unsupported language: ", lang);
			}
		}
		catch (PSOATransRunException e) {
			printErrlnAndExit(e.getMessage());
		}

		PSOATransRun system = new PSOATransRun(translator, engine);
		system.setPrintTrans(outputTrans);
		
		// Run tests
		if (isTest)
		{			
			try {
				TestSuite ts = new TestSuite(kbFile, system, numRuns);
				ts.run();
				ts.outputSummary();
				System.exit(0);
			}
			catch (PSOATransRunException e)
			{
				printErrlnAndExit(e.getMessage());
			}
			// TestSuite already calls system.shutdown(), hence
			// no finally needed
		}
		
		// Print initial PSOA KB if requested
		if (showOrigKB) {
			println("Original KB:");
			try (BufferedReader reader = new BufferedReader(
					new FileReader(kbFile))) {
				do {
					String line = reader.readLine();
					if (line != null)
						println(line);
					else
						break;
				} while (true);
			}

			println();
		}
		
		try {
			// Load KB
			system.loadKB(kbStream);
			println("KB Loaded");

			if (queryStream != null) {
				// Execute query from file input
				QueryResult result = system.executeQuery(queryStream);
				printQueryResult(result, getAllAnswers, null);
			}
			else {
				try (Scanner sc = new Scanner(System.in)) {
					// Console query loop
					do {
						println("Input Query:");
						if (!sc.hasNext())
							break;

						String query = sc.nextLine();
						QueryResult result = system.executeQuery(query);
						printQueryResult(result, getAllAnswers, sc);
						println();
					} while (true);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			system.shutdown();
		}
	}

	private static void printQueryResult(QueryResult result,
			boolean getAllAnswers, Scanner reader) {
		println("Answer(s):");

		if (getAllAnswers) {
			println(result);
		}
		else {
			AnswerIterator iter = result.iterator();
			try {
				if (!iter.hasNext()) {
					println("No");
				}
				else {
					// Output first answer
					Substitution answer = iter.next();
					print(answer, "\t");
	
					// Handle user requests for more answers if the first answer is not "yes"
					if (!answer.isEmpty()) {
						while(true) {
							String input = reader.nextLine();
							if (input.equals(";")) {
								if (iter.hasNext())
									print(iter.next(), "  \t");
								else {
									print("No");
									break;
								}
							}
							else if (input.isEmpty()) {
								println(iter.hasNext()? "Yes" : "No");
								break;
							}
						}
					}
					
					println();
				}
			}
			finally
			{
				iter.dispose();
			}
		}
	}
	
	private static void printErrlnAndExit(Object... objs)
	{
		printErrln(objs);
		System.exit(1);
	}
	
	private static void printUsage(boolean isLong) {
		println("Usage: java -jar PSOATransRun.jar -i <kb> [-e] [-p] [-o <translated KB output>] [-q <query>] [-a] [-s] [-x <xsb folder>]");
		println("Options:");
		println("  -?,--help         Print the help message");
		println("  -a,--allAns       Retrieve all answers for each query at once");
		println("  -i,--input        Input Knowledge Base (KB)");
		println("  -e,--echoInput    Echo input KB to standard output");
		println("  -q,--query        Query document for the KB. If the query document");
		println("                    is not specified, the engine will read queries");
		println("                    from the standard input.");
		println("  -p,--printTrans   Print translated KB and queries to standard output");
		println("  -o,--outputTrans  Save translated KB to the designated file");
		println("  -x,--xsbfolder    Specifies XSB installation folder. The default path is ");
		println("                    obtained from the environment variable XSB_DIR");
		println("  -u,--undiff       Perform undifferentiated objectification");
		println("  -s,--staticOnly   Perform static objectification only");
		
		if (isLong)
		{
			println("     --longhelp     Print the help message, including commands for internal use");
			println("  -l,--targetLang   Translation target language (currently support");
			println("                    \"prolog\" and \"tptp\")");
			println("  -t,--test         Run PSOATransRun tests (-i specifies the directory");
			println("                    for the test suite)");
			println("  -n,--numRuns      Number of runs for each test case");
			println("  -m,--timeout      Timeout (only supported for the TPTP instantiation");
			println("                    of PSOATransRun)");
			println("  -z,--omitNegMem   Omit memterm in the slotribution of negative occurrences");
			println("                    of psoa atoms with at least one dependent descriptor");
		}
	}
}
