package com.leidos.bmech.model;

import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Handle the programs arguments for DRAY.
 */
public class CommandLineValues {
  
  // Batch mode.
  @Option(name = "-b", 
          aliases = { "--batch", "--nogui" }, 
          required = false, 
          usage = "Run in batch mode. Do not start graphical interface.")
  private boolean batch;

  // PDF source file.
  @Option(name = "-f", 
          aliases = { "--pdf", "--file" }, 
          required = false, 
          usage = "PDF file to load.")
  private File    source;

  // Output JSON representation.
  @Option(name = "-o", 
          aliases = {"--out" }, 
          required = false, usage = "Specify JSON output file (optional).")
  private String  out       = "DEFAULT";

  // Help.
  @Option(name = "-h", 
          aliases = { "--help" }, 
          required = false, 
          usage = "Print this usage message.")
  private boolean help;

  private boolean errorFree = false;
  
  private boolean hasInput  = false;
  
  private File    outFile;

  @SuppressWarnings("static-access")
  
  public CommandLineValues(String... args) {
    
    CmdLineParser parser = new CmdLineParser(this);
    parser.setUsageWidth(80);
    
    try {
      parser.parseArgument(args);
      if (help) {
        parser.printUsage(System.out);
        System.exit(0);
      } else {
        
        if (getFile() != null) {
          hasInput = true;
          source = new File(source.getCanonicalPath().replace("\\", "/"));
          if (!getFile().exists()) {
            throw new CmdLineException(parser, "Can't open " + getFile() + ". It's not a file or directory.");
          }
        }

        if (!out.equals("DEFAULT")) {
          outFile = new File(getFile().getParent() + getFile().separatorChar + out);

          if (!new File(getOutFile().getParent()).exists()) {
            throw new CmdLineException(parser, "Can't write to " + getOutFile() + ". Parent dir does not exist.");
          }
        }

      }
      errorFree = true;
      
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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

  /**
   * Returns the source file.
   *
   * @return The source file.
   */
  public File getFile() {
    return source;
  }

  public File getOutFile() {
    return outFile;
  }

  public boolean isBatch() {
    return batch;
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
