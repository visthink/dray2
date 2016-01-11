package com.leidos.bmech.model;

import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * This class handles the programs arguments.
 */
public class CommandLineValues {
    @Option(name = "-g", aliases = { "--gui" }, required = false,
            usage = "boolean flag controls whether the gui is run")
    private boolean gui;
    
    @Option(name = "-f", aliases = { "--pdf" , "--file"}, required=false,
    		usage = "pdf file to load")
    private File source;
    
    @Option(name = "-o", aliases = { "--out" }, required=false,
    		usage = "(OPTIONAL) output json file relative to the input file")
    private String out = "DEFAULT";
    
    @Option(name = "-h", aliases = { "--help" }, required=false,
    		usage = "print usage message")
    private boolean help;


    private boolean errorFree = false;
    private boolean hasInput = false;
    private File outFile;
    public CommandLineValues(String... args) {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);
            if(help){
            	parser.printUsage(System.out);
            } else {
            
            	if (getFile()!=null){
            		hasInput = true;
            		source = new File(source.getCanonicalPath().replace("\\", "/"));
            		if (!getFile().exists()) {
    	                throw new CmdLineException(parser,
    	                        "Can't open "+getFile()+". It's not a file or directory.");
    	            }
            	}
	            
	            if(!out.equals("DEFAULT")){
	            	outFile = new File(getFile().getParent() + getFile().separatorChar +  out);
		            
		            if (!new File(getOutFile().getParent()).exists()){
		                throw new CmdLineException(parser,
		                        "Can't write to "+getOutFile()+". Parent dir does not exist.");
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
     * Returns whether the parameters could be parsed without an
     * error.
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
    
    public boolean isGui() {
    	return gui;
    }
    public boolean isHelp() {
    	return help;
    }
    public boolean hasInput() {
    	return hasInput;
    }
}
