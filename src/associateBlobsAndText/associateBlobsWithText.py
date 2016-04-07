import xml.etree.ElementTree
import sys

import sexpParser


SufficientlyContained = 0.9


def liesBetween(x, a, b):
    return (x >= a) and (x <= b)


def liesInside(x, y, bbX, bbY, bbWidth, bbHeight):
    return liesBetween(x, bbX, bbX + bbWidth) and \
        liesBetween(y, bbY, bbY + bbHeight)


def computeOverlapBoundingBox(bb1X, bb1Y, bb1Width, bb1Height, 
                              bb2X, bb2Y, bb2Width, bb2Height):
    overlapX = 0.0
    overlapY = 0.0
    overlapWidth = 0.0
    overlapHeight = 0.0

    if liesBetween(bb1X, bb2X, bb2X + bb2Width):
        overlapX = bb1X
    elif liesBetween(bb2X, bb1X, bb1X + bb1Width):
        overlapX = bb2X

    if liesBetween(bb1X + bb1Width, bb2X, bb2X + bb2Width):
        overlapWidth = bb1X + bb1Width - overlapX
    elif liesBetween(bb2X + bb2Width, bb1X, bb1X + bb1Width):
        overlapWidth = bb2X + bb2Width - overlapX

    if liesBetween(bb1Y, bb2Y, bb2Y + bb2Height):
        overlapY = bb1Y
    elif liesBetween(bb2Y, bb1Y, bb1Y + bb1Height):
        overlapY = bb2Y

    if liesBetween(bb1Y + bb1Height, bb2Y, bb2Y + bb2Height):
        overlapHeight = bb1Y + bb1Height - overlapY
    elif liesBetween(bb2Y + bb2Height, bb1Y, bb1Y + bb1Height):
        overlapHeight = bb2Y + bb2Height - overlapY
        
    return (overlapX, overlapY, overlapWidth, overlapHeight)


def addBlobsToXml(blobs, outputRoot, scaleFactorX, scaleFactorY):
    for blob in blobs:
        blobId = blob[1]
        blobBbTopLeftX = float(blob[7][0]) * scaleFactorX
        blobBbTopLeftY = float(blob[7][1]) * scaleFactorY
        blobBbWidth = float(blob[7][2]) * scaleFactorX
        blobBbHeight = float(blob[7][3]) * scaleFactorY
        blobGroupIndex = blob[25]
        
        blobTag = xml.etree.ElementTree.SubElement(outputRoot, 'Blob')
        blobTag.set('blob-id', str(blobId))
        blobTag.set('x', str(blobBbTopLeftX))
        blobTag.set('y', str(blobBbTopLeftY))
        blobTag.set('w', str(blobBbWidth))
        blobTag.set('h', str(blobBbHeight))
        blobTag.set('group-index', str(blobGroupIndex))
        blobTag.text = str(blobGroupIndex)


def addLabelsToXml(tokenTags, outputRoot, imageX, imageY):
    for tokenTag in tokenTags:
        textId = tokenTag.get('id')
        textX = (float(tokenTag.get('x')) - imageX)
        textY = (float(tokenTag.get('y')) - imageY)
        textWidth = float(tokenTag.get('width'))
        textHeight = float(tokenTag.get('height'))
        fontName = tokenTag.get('font-name')
        fontSize = float(tokenTag.get('font-size'))
        isBold = tokenTag.get('bold')
        isItalic = tokenTag.get('italic')
        text = tokenTag.text

        labelTag = xml.etree.ElementTree.SubElement(outputRoot, 'Label')
        labelTag.set('token-id', str(textId))
        labelTag.set('x', str(textX))
        labelTag.set('y', str(textY))
        labelTag.set('w', str(textWidth))
        labelTag.set('h', str(textHeight))
        labelTag.set('font-name', fontName)
        labelTag.set('font-size', str(fontSize))
        labelTag.set('bold', isBold)
        labelTag.set('italic', isItalic)
        labelTag.text = text


def addFreeLabelsToXml(tokenTags, outputRoot, imageX, imageY, 
                       labelsAssociatedWithBlobs):
    for tokenTag in tokenTags:
        textId = tokenTag.get('id')
        textX = (float(tokenTag.get('x')) - imageX)
        textY = (float(tokenTag.get('y')) - imageY)
        textWidth = float(tokenTag.get('width'))
        textHeight = float(tokenTag.get('height'))
        fontName = tokenTag.get('font-name')
        fontSize = float(tokenTag.get('font-size'))
        isBold = tokenTag.get('bold')
        isItalic = tokenTag.get('italic')
        text = tokenTag.text

        if textId not in labelsAssociatedWithBlobs:
            freeLabelTag = xml.etree.ElementTree.SubElement(outputRoot, 
                                                            'Free-label')
            freeLabelTag.set('token-id', str(textId))
            freeLabelTag.set('x', str(textX))
            freeLabelTag.set('y', str(textY))
            freeLabelTag.set('w', str(textWidth))
            freeLabelTag.set('h', str(textHeight))
            freeLabelTag.set('font-name', fontName)
            freeLabelTag.set('font-size', str(fontSize))
            freeLabelTag.set('bold', isBold)
            freeLabelTag.set('italic', isItalic)
            freeLabelTag.text = text


def associateBlobsWithText(blobFile, textFile, outputFile):
    with open(blobFile, 'r') as blobData:
        # Parse the blob file as an s-expression
        blobz = sexpParser.sexp.parseString(blobData.read(), parseAll=True)
        originalBlobFilename = blobz[0][1]
        rasterHeight = blobz[0][3]
        rasterWidth = blobz[0][5]
        numBlobs = int(blobz[0][7])
        numBlobGroups = int(blobz[0][9])
        blobs = blobz[0][11]
        
        # Parse the XML file containing the text information
        pdfTree = xml.etree.ElementTree.parse(textFile)
        pdfRoot = pdfTree.getroot()

        # Get PDF image size and location on PDF page
        imageTag = pdfRoot.find('IMAGE')
        imageWidth = float(imageTag.get('width'))
        imageHeight = float(imageTag.get('height'))
        imageX = float(imageTag.get('x'))
        imageY = float(imageTag.get('y'))
        
        # Compute some scale factors
        scaleFactorX = imageWidth / rasterWidth
        scaleFactorY = imageHeight / rasterHeight

        # Start building the output XML structure
        outputRoot = xml.etree.ElementTree.Element('Output')

        # Add all blobs to the XML
        addBlobsToXml(blobs, outputRoot, scaleFactorX, scaleFactorY)

        # Add all labels to the XML
        addLabelsToXml(pdfRoot.iter('TOKEN'), outputRoot, imageX, imageY)

        # Keep track of which labels are associated with blobs
        labelsAssociatedWithBlobs = []

        # Loop over blobs
        for blob in blobs:
            blobId = blob[1]
            blobBbTopLeftX = float(blob[7][0]) * scaleFactorX
            blobBbTopLeftY = float(blob[7][1]) * scaleFactorY
            blobBbWidth = float(blob[7][2]) * scaleFactorX
            blobBbHeight = float(blob[7][3]) * scaleFactorY
            blobGroupIndex = blob[25]

            # Loop over pieces of text
            for tokenTag in pdfRoot.iter('TOKEN'):
                textId = tokenTag.get('id')
                textX = (float(tokenTag.get('x')) - imageX)
                textY = (float(tokenTag.get('y')) - imageY)
                textWidth = float(tokenTag.get('width'))
                textHeight = float(tokenTag.get('height'))
                fontName = tokenTag.get('font-name')
                fontSize = float(tokenTag.get('font-size'))
                isBold = tokenTag.get('bold')
                isItalic = tokenTag.get('italic')
                text = tokenTag.text
                
                # Compute the overlap between the text and blob
                # bounding boxes
                overlapBb = computeOverlapBoundingBox(textX, textY, 
                                                      textWidth, 
                                                      textHeight, 
                                                      blobBbTopLeftX, 
                                                      blobBbTopLeftY, 
                                                      blobBbWidth, 
                                                      blobBbHeight)
                fractionalOverlap = (overlapBb[2] * overlapBb[3]) / \
                    (textWidth * textHeight)
                if fractionalOverlap >= SufficientlyContained:
                    # If the bounding box for the text overlaps the
                    # blob's bounding box significantly, then
                    # associate the text with the blob as a
                    # "Contained-label"
                    clTag = xml.etree.ElementTree.SubElement(outputRoot, 
                                                             'Contained-label')
                    # Set the blob-related properties
                    clTag.set('blob-id', str(blobId))
                    clTag.set('x', str(blobBbTopLeftX))
                    clTag.set('y', str(blobBbTopLeftY))
                    clTag.set('w', str(blobBbWidth))
                    clTag.set('h', str(blobBbHeight))
                    clTag.set('group-index', str(blobGroupIndex))
                    # Set the text-related properties
                    clTag.set('token-id', str(textId))
                    clTag.set('font-name', fontName)
                    clTag.set('font-size', str(fontSize))
                    clTag.set('bold', isBold)
                    clTag.set('italic', isItalic)
                    clTag.text = u'{0}'.format(text)

                    labelsAssociatedWithBlobs.append(textId)
                elif fractionalOverlap > 0.0:
                    # If the bounding box for the text overlaps the
                    # blob's bounding box somewhat, then associate the
                    # text with the blob as an "Overlapping-label"
                    olTag = xml.etree.ElementTree.SubElement(outputRoot, 
                                                             'Overlapping-label')
                    # Set the blob-related properties
                    olTag.set('blob-id', str(blobId))
                    # Ron wants the mounding box here to be the union
                    # of the blob and label bounding boxes
                    minX = min(blobBbTopLeftX, textX)
                    maxX = max(blobBbTopLeftX + blobBbWidth, 
                               textX + textWidth)
                    minY = min(blobBbTopLeftY, textY)
                    maxY = max(blobBbTopLeftY + blobBbHeight, 
                               textY + textHeight)
                    olTag.set('x', str(minX))
                    olTag.set('y', str(minY))
                    olTag.set('w', str(maxX - minX))
                    olTag.set('h', str(maxY - minY))
                    olTag.set('group-index', str(blobGroupIndex))
                    # Set the text-related properties
                    olTag.set('token-id', str(textId))
                    olTag.set('font-name', fontName)
                    olTag.set('font-size', str(fontSize))
                    olTag.set('bold', isBold)
                    olTag.set('italic', isItalic)
                    olTag.text = u'{0}'.format(text)

                    labelsAssociatedWithBlobs.append(textId)

        # Add the free labels to the XML
        addFreeLabelsToXml(pdfRoot.iter('TOKEN'), outputRoot, imageX, imageY, 
                           labelsAssociatedWithBlobs)
                        
        # Wrap the XML structure in an ElementTree instance, and save
        # as XML
        outputTree = xml.etree.ElementTree.ElementTree(outputRoot)
        outputTree.write(outputFile)


def main():
    blobFile = sys.argv[1]
    textFile = sys.argv[2]
    outputFile = sys.argv[3]

    associateBlobsWithText(blobFile, textFile, outputFile)


if __name__ == '__main__':
    main()
