
# Notes on running the scripts
 
# get the following files and put them in the same directory
#             associateBlobsWithText.py
#             sexpParser.py
#             PointAndRectangle.py
#             TextExtractorXML.py
# assuming you have
#             Yanlin's blob output file  $YANLIN_BLOB_FILE
#             and the pdf xml file from pdf2xml $XML_PDF
 
 
# run TextExtractorXML.py on the pdf xml. The 2nd argument (text_overlays.xml) is the output file
python TextExtractorXML.py $XML_PDF text_overlays.xml
 
# run associateBlobsAndText on the output of that and Yanlin�s blob file 
python associateBlobsAndText.py $YANLIN_BLOB_FILE text_overlays.xml output.xml
 



 
 