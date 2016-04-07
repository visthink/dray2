import xml.etree.ElementTree as xml
from PointAndRectangle import *
import sys, os


def elem2rect(el):
    p1 = Point(float(el.get('x')), float(el.get('y')))
    p2 = Point(float(el.get('x')) + float(el.get('width')), float(el.get('y')) + float(el.get('height')))
    return Rect(p1, p2)

if __name__=="__main__":
    in_filename = sys.argv[1]
    tree = xml.parse(in_filename)
    document = tree.getroot()
    pages = tree.findall("PAGE")
    new_root = xml.Element("IMAGE_LIST")
    new_tree = xml.ElementTree(new_root)
    for page in pages:
        #add the page dims to the imagelist (assumes 1 page per list)
        new_root.attrib["page_width"] = page.attrib.get("width")
        new_root.attrib["page_height"] = page.attrib.get("height")
        images = page.findall('IMAGE')
        for image in images: 
            img_rect = elem2rect(image)     
            texts = page.findall('TEXT')
            for text in texts:
                text_rect = elem2rect(text)
                if text_rect.overlaps(img_rect):
                    image.append(text)
                    '''tokens =  text.findall('TOKEN')
                    for token in tokens:
                        print token.text
                    print ""'''
            new_root.append(image)
    if(len(sys.argv) > 2):
        new_tree.write(sys.argv[2])
    else:
        new_tree.write(os.path.splitext(in_filename)[0] + '_overlays.xml')
