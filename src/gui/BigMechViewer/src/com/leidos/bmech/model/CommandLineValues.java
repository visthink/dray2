package com.leidos.bmech.model;

import java.io.File;
//import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.Argument;
//import dray.j.Producer.Table;


/**
 * Command-line handler for DRAY. Called at beginning of DRAY run when called from the command line.
 * 
 */
public class CommandLineValues {

  // Batch mode.
  @Option(name = "-b", 
//          aliases = { "--batch", "--nogui" }, 
          required = false, 
          usage = "Run in batch mode. Do not start graphical interface.")
  private boolean      batch;

  // Output JSON representation.
  @Option(name = "-o", 
 //         aliases = { "--out" }, 
          required = false, 
          metaVar = "JSON-filename",
          usage = "JSON output filename or path (optional).")
  private String       out               = "DEFAULT";

  // Help.
  @Option(name = "-h", 
          aliases = {"-?", "-help"}, 
          required = false, 
          usage = "Print this usage message and exit.")
  private boolean      help;

  // Skip overlays
  @Option(name = "-x",
//          aliases = {"--skipoverlays"},
          required = false,
          usage = "Skip loading working set overlays, even if available."
          )
  private boolean skipOverlays = false;
  
//  // Configuration file to use.
//  @Option(name = "-c",
////          aliases = {"--config"},
//          metaVar = "config-file",
//          required = false,
//          usage = "Configuration file to use instead of default.")
//  private String configFileString = "";
  
//  @Option(name = "-p",
// //         aliases = {"--producer"},
//          required = false,
//          metaVar = "producer-list",
//          usage = "Bracketed list of lists with working set tags and producers.")
//  private String producers = "";

  // Should we run the table detection code?
  @Option(name = "-t",
		  required = false,
		  usage = "Run the table detection code on all table working sets in the loaded file.")
  private boolean useTableCode;

  // Single argument - PDF file or directory with PDF files.
  @Argument(index = 0,
            required = false,
            metaVar = "source",
            usage = "PDF file (or directory containing PDF files).")
  String sourceName = "";
  
  
  private boolean      errorFree         = true;

  private boolean      hasInput          = true;

  private File         outFile;

  private File         source;                                     // Single PDF
                                                                   // file or
  private boolean      sourceIsDirectory = false;                  // Assume
                                                                   // file until
                                                                   // told
                                                                   // otherwise.
  private List<?>      producerList;

  @SuppressWarnings("static-access")

  public CommandLineValues(String... args) {

    CmdLineParser parser = new CmdLineParser(this);
    parser.setUsageWidth(80);

    try {

      parser.parseArgument(args);

    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);

    }

    // Handle help request
    
    if (help) {
      System.out.println("");
      System.out.println("Usage: dray <options> source");
      System.out.println("");
      parser.printUsage(System.out);
      System.exit(0);
    }

    // Determine source file or directory, if needed.
   
    // System.out.println("sourceName is " + sourceName.toString());
    
    if (sourceName == "") {
      hasInput = false;
    } else {
      hasInput = true;
      source = new File(sourceName);      
    }
   
    if (sourceName == "") {
      
      // SKIP
      
    } else if (!source.exists()) {
      
      System.out.println("Source " + source.toString() + " doesn't exist.");
      System.exit(-1);
      
    } else if (source.isFile()) {
      
      // SKIP
      
    } else if (source.isDirectory()) {
      
        sourceIsDirectory = true;
        
    } else { // Should never reach here unless there's a problem with the code.
      
        System.out.println("Cannot identify the file status of " + source.toString());
        System.exit(-1);
    }

    if (!out.equals("DEFAULT")) {
      
      outFile = new File(getFile().getParent() + getFile().separatorChar + out);

      if (!new File(getOutFile().getParent()).exists()) {
        System.out.println("Can't write to " + getOutFile() + ". Parent directory does not exist.");
        System.exit(-1);
      }
    }

    // Handle the set of producers to run.
//    if (producers == "") {
//      producerList = new ArrayList<Object>();
//    } else {
//      producerList = Table.parseProducerList(producers);
//    }
//    System.out.println("Producers are " + producerList.toString());
    
    if (getFile() != null) {
      hasInput = true;
    }

  }

  /**
   * Returns whether the parameters could be parsed without an error.
   *
   * @return true if no error occurred.
   */
  public boolean isErrorFree() {
    return errorFree;
  }

  public File getFile() {
    return source;
  }

  public boolean sourceIsDirectory() {
    return sourceIsDirectory;
  }
  
  public boolean skipOverlays() {
    return skipOverlays;
  }

  public File getOutFile() {
    return outFile;
  }

  public boolean isBatch() {
    return batch;
  }
  
  public boolean isTableCode() {
	return useTableCode;
  }

  /**
   * @deprecated Use isBatch() instead.
   * @return true if GUI should be started.
   */
  @Deprecated
  public boolean isGui() {
    return !isBatch();
  }

  public boolean isHelp() {
    return help;
  }

  public boolean hasInput() {
    return hasInput;
  }
}
