// Copyright 2002-2004 Elliotte Rusty Harold
// 
// This library is free software; you can redistribute 
// it and/or modify it under the terms of version 2.1 of 
// the GNU Lesser General Public License as published by  
// the Free Software Foundation.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General 
// Public License along with this library; if not, write to the 
// Free Software Foundation, Inc., 59 Temple Place, Suite 330, 
// Boston, MA  02111-1307  USA
// 
// You can contact Elliotte Rusty Harold by sending e-mail to
// elharo@metalab.unc.edu. Please include the word "XOM" in the
// subject line. The XOM home page is temporarily located at
// http://www.cafeconleche.org/XOM/  but will eventually move
// to http://www.xom.nu/
package nu.xom.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.NodeFactory;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.ParsingException;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import nu.xom.Text;
import nu.xom.xslt.XSLException;
import nu.xom.xslt.XSLTransform;

/**
 * <p>
 *  Unit tests for the XSLT engine.
 * </p>
 * 
 * @author Elliotte Rusty Harold
 * @version 1.0d23
 *
 */
public class XSLTransformTest extends XOMTestCase {

    
    public XSLTransformTest(String name) {
        super(name);   
    }

    
    // not a literal result element as stylesheet 
    // because it's missing the xsl:version attribute
    private String notAStyleSheet = 
     "<?xml-stylesheet href=\"file.css\" type=\"text/css\"?>" 
     + "<!-- test -->"
     + "<test xmlns:xlink='http://www.w3.org/TR/1999/xlink'>Hello dear"
     + "\r\n<em id=\"p1\" xmlns:none=\"http://www.example.com\">"
     + "very important</em>"
     + "<span xlink:type='simple'>here&apos;s the link</span>\r\n"
     + "<svg:svg xmlns:svg='http://www.w3.org/TR/2000/svg'>"
     + "<svg:text>text in a namespace</svg:text></svg:svg>\r\n"
     + "<svg xmlns='http://www.w3.org/TR/2000/svg'>"
     + "<text>text in a namespace</text></svg>"
     + "</test>\r\n"
     + "<!--epilog-->";

    
    public void testReaderConstructor() {
        
        try {
            new XSLTransform(new StringReader(notAStyleSheet));
            fail("Compiled non-stylesheet");
        }
        catch (XSLException success) {
            assertNotNull(success.getMessage());
        }
        
    }

    
    public void testInputStreamConstructor() throws IOException {
        
        try {
            byte[] data = notAStyleSheet.getBytes("UTF-8");
            new XSLTransform(new ByteArrayInputStream(data));
            fail("Compiled non-stylesheet");
        }
        catch (XSLException success) {
            assertNotNull(success.getMessage());
        }
        
    }

    
    public void testDocumentConstructor() 
      throws ParsingException, IOException {
        
        try {
            Builder builder = new Builder();
            Document doc = builder.build(notAStyleSheet, 
              "http://www.example.com");
            new XSLTransform(doc);
            fail("Compiled non-stylesheet");
        }
        catch (XSLException success) { 
            assertNotNull(success.getMessage());
        }
        
    }

    
    public void testFileConstructor() {
        
        try {
            File f = new File("data/schematest.xml");
            new XSLTransform(f);
            fail("Compiled non-stylesheet");
        }
        catch (XSLException success) {  
            assertNotNull(success.getMessage());
        }
        
    }

    
    public void testURLConstructor() throws IOException {
        
        try {
            File f = new File("data/schematest.xml");
            new XSLTransform(f.toURL().toExternalForm());
            fail("Compiled non-stylesheet");
        }
        catch (XSLException success) {  
            assertNotNull(success.getMessage());
        }
        
    }


/*   <xsl:template match="/">
    <element1>some data and <content/> for a test</element1>
    <element2>Remember, the XSLT processor is going to strip out the literal white space</element2>
    <element3>some data and <content/> for a <!--test--></element3>
    <element4/>
    <xsl:comment>test</xsl:comment>
    <xsl:processing-instruction name="test">PIs are not treated as literals in XSLT?</xsl:processing-instruction>
  </xsl:template> */
  
    public void testCreateDocumentFragment() 
      throws ParsingException, IOException, XSLException {
        
        Element element1 = new Element("element1");
        element1.appendChild("some data and ");
        element1.appendChild(new Element("content"));
        element1.appendChild(" for a test");
        
        Element element2 = new Element("element2");
        element2.appendChild(
          "Remember, the XSLT processor is going to strip out the literal white space"
        );
        File doc = new File("data/xslt/input/8-14.xml");
        File stylesheet = new File("data/xslt/input/fragment.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        Document input = builder.build(doc);
        Nodes output = xform.transform(input);
        assertEquals(6, output.size());
        assertEquals(element1, output.get(0));
        assertEquals(element2, output.get(1));
        assertEquals(new Element("element4"), output.get(3));
        assertEquals(new Comment("test"), output.get(4));
        assertEquals(new ProcessingInstruction("test", 
          "PIs are not treated as literals in XSLT?"), output.get(5));
        
    }

    
    public void testTransform() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-1.xml");
        File stylesheet = new File("data/xslt/input/8-8.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        Nodes output = xform.transform(builder.build(doc));
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));

        Document expected = builder.build("data/xslt/output/8-8.xml");
        assertEquals(expected, result);
        
    }

    
    public void testTransformWithCFilter() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-1.xml");
        File stylesheet = new File("data/xslt/input/8-8.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactoryTest.CFactory());
        
        Nodes output = xform.transform(builder.build(doc));
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));

        Document expected = builder.build("data/xslt/output/8-8c.xml");
        assertEquals(expected, result);
        
    }

    
    public void testCreateDocumentFragmentWithCommentFilter() 
      throws ParsingException, IOException, XSLException {
        
        Element element1 = new Element("element1");
        element1.appendChild("some data and ");
        element1.appendChild(new Element("content"));
        element1.appendChild(" for a test");
        
        Element element2 = new Element("element2");
        element2.appendChild(
          "Remember, the XSLT processor is going to strip out the literal white space"
        );
        File doc = new File("data/xslt/input/8-14.xml");
        File stylesheet = new File("data/xslt/input/fragment.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactoryTest.CommentFilter());
        
        Document input = builder.build(doc);
        Nodes output = xform.transform(input);
        assertEquals(5, output.size());
        assertEquals(element1, output.get(0));
        assertEquals(element2, output.get(1));
        assertEquals(new Element("element4"), output.get(3));
        assertEquals(new ProcessingInstruction("test", 
          "PIs are not treated as literals in XSLT?"), output.get(4));
        
    }    
    
    
    public void testCreateDocumentFragmentWithProcessingInstructionFilter() 
      throws ParsingException, IOException, XSLException {
        
        Element element1 = new Element("element1");
        element1.appendChild("some data and ");
        element1.appendChild(new Element("content"));
        element1.appendChild(" for a test");
        
        Element element2 = new Element("element2");
        element2.appendChild(
          "Remember, the XSLT processor is going to strip out the literal white space"
        );
        File doc = new File("data/xslt/input/8-14.xml");
        File stylesheet = new File("data/xslt/input/fragment.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactoryTest.ProcessingInstructionFilter());
        
        Document input = builder.build(doc);
        Nodes output = xform.transform(input);
        assertEquals(5, output.size());
        assertEquals(element1, output.get(0));
        assertEquals(element2, output.get(1));
        assertEquals(new Element("element4"), output.get(3));
        assertEquals(new Comment("test"), output.get(4));
        
    }    
    
    
    public void testCreateDocumentFragmentWithUncommentFilter() 
      throws ParsingException, IOException, XSLException {
        
        Element element1 = new Element("element1");
        element1.appendChild("some data and ");
        element1.appendChild(new Element("content"));
        element1.appendChild(" for a test");
        
        Element element2 = new Element("element2");
        element2.appendChild(
          "Remember, the XSLT processor is going to strip out the literal white space"
        );
        File doc = new File("data/xslt/input/8-14.xml");
        File stylesheet = new File("data/xslt/input/fragment.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactoryTest.UncommentFilter());
        
        Document input = builder.build(doc);
        Nodes output = xform.transform(input);
        assertEquals(6, output.size());
        assertEquals(element1, output.get(0));
        assertEquals(element2, output.get(1));
        assertEquals(new Element("element4"), output.get(3));
        assertEquals(new Text("test"), output.get(4));
        assertEquals(new ProcessingInstruction("test", 
          "PIs are not treated as literals in XSLT?"), output.get(5));
        
    }    
    
    
    public void testTransform2() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-1.xml");
        File stylesheet = new File("data/xslt/input/8-12.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        Nodes output = xform.transform(builder.build(doc));
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));

        Document expected = builder.build("data/xslt/output/8-12.xml");
        assertEquals(expected, result);
        
    }

    
    public void testTransformFromInputStream() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-1.xml");
        InputStream stylesheet = new FileInputStream("data/xslt/input/8-12.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        Nodes output = xform.transform(builder.build(doc));
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));
        
        Document expected = builder.build("data/xslt/output/8-12.xml");
        assertEquals(expected, result);
        
    }

    
    public void testTransformFromReader() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-1.xml");
        Reader stylesheet = new FileReader("data/xslt/input/8-12.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        Nodes output = xform.transform(builder.build(doc));
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));
        Document expected = builder.build("data/xslt/output/8-12.xml");
        assertEquals(expected, result);
        
    }

    
    // For debugging
    private static void dumpResult(Document result, String filename) 
      throws IOException {
        File debug = new File("data/xslt/debug/" + filename);
        OutputStream out = new FileOutputStream(debug);
        Serializer serializer = new Serializer(out);
        serializer.write(result);
        serializer.flush();
        out.close();
    }

    
    public void testTransformFromDocument() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-1.xml");
        Builder builder = new Builder();
        Document stylesheet = builder.build("data/xslt/input/8-12.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        Nodes output = xform.transform(builder.build(doc));
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));

        Document expected = builder.build("data/xslt/output/8-12.xml");
        assertEquals(expected, result);
        
    }

    
    public void testTransformFromSystemID() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-1.xml");
        Builder builder = new Builder();
        String stylesheet = "data/xslt/input/8-12.xsl";
        XSLTransform xform = new XSLTransform(stylesheet);
        Nodes output = xform.transform(builder.build(doc));
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));

        Document expected = builder.build("data/xslt/output/8-12.xml");
        assertEquals(expected, result);
        
    }



    public void testTransformWithNamespaces() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-14.xml");
        File stylesheet = new File("data/xslt/input/8-15.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        Document input = builder.build(doc);
        Nodes output = xform.transform(input);
        assertEquals(1, output.size());
        Document result = new Document((Element) (output.get(0)));

        Document expected = builder.build("data/xslt/output/8-15.xml");
        assertEquals(expected, result);
        
    }

    public void testSingleTextNode() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-14.xml");
        File stylesheet = new File("data/xslt/input/singlestring.xsl");
        Builder builder = new Builder();
        XSLTransform xform = new XSLTransform(stylesheet);
        Document input = builder.build(doc);
        Nodes output = xform.transform(input);
        assertEquals(1, output.size());
        Text data = (Text) (output.get(0));
        assertEquals("Data", data.getValue());
        
    }
    
    public void testToString() throws XSLException {
        
        File stylesheet = new File("data/xslt/input/singlestring.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        assertTrue(xform.toString().startsWith("[nu.xom.xslt.XSLTransform: "));
        
    }    
    

    // Make sure that method="text" doesn't affect what we get
    // since this is not a serialized transform
    public void testTextMethod() 
      throws ParsingException, IOException, XSLException {
        
        File doc = new File("data/xslt/input/8-14.xml");
        File stylesheet = new File("data/xslt/input/textmethod.xsl");
        Builder builder = new Builder(); 
        XSLTransform xform = new XSLTransform(stylesheet);
        Document input = builder.build(doc);
        Nodes output = xform.transform(input);
        assertEquals(6, output.size());
        assertEquals("12345", output.get(0).getValue());
        assertEquals("67890", output.get(1).getValue());
        assertEquals("", output.get(2).getValue());
        assertEquals("0987654321", output.get(3).getValue());
        assertTrue(output.get(4) instanceof Comment);
        assertTrue(output.get(5) instanceof ProcessingInstruction);
    }

    public void testCommentWithParent() throws XSLException {
        
        File stylesheet = new File("data/xslt/input/commentwithparent.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        Document input = new Document(new Element("root"));
        Nodes output = xform.transform(input);
        assertEquals(1, output.size());
        assertEquals("", output.get(0).getValue());
        Element root = (Element) output.get(0);
        assertEquals(1, root.getChildCount());
        Comment child = (Comment) root.getChild(0);
        assertEquals("test", child.getValue());
    }

    public void testProcessingInstructionWithParent() 
      throws XSLException {
        
        File stylesheet = new File("data/xslt/input/piwithparent.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        Document input = new Document(new Element("root"));
        Nodes output = xform.transform(input);
        assertEquals(1, output.size());
        assertEquals("", output.get(0).getValue());
        Element root = (Element) output.get(0);
        assertEquals(1, root.getChildCount());
        ProcessingInstruction child = (ProcessingInstruction) root.getChild(0);
        assertEquals("target", child.getTarget());
        assertEquals("test", child.getValue());
    } 

    public void testTransformNodes() throws XSLException {
        
        File stylesheet = new File("data/xslt/input/piwithparent.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        Nodes input = new Nodes(new Element("root"));
        Nodes output = xform.transform(input);
        assertEquals(1, output.size());
        assertEquals("", output.get(0).getValue());
        Element root = (Element) output.get(0);
        assertEquals(1, root.getChildCount());
        ProcessingInstruction child = (ProcessingInstruction) root.getChild(0);
        assertEquals("target", child.getTarget());
        assertEquals("test", child.getValue());
    } 

    
    // primarily this makes sure the XSLTHandler can handle various
    // edge cases
    public void testIdentityTransform() throws XSLException {
        
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        Element root = new Element("root", "http://www.example.org");
        root.appendChild(new Text("some data"));
        root.appendChild(new Element("something"));
        root.addAttribute(new Attribute("test", "test"));
        root.addAttribute(new Attribute("pre:red", "http://www.red.com/", "value"));
        Document input = new Document(root);
        Nodes output = xform.transform(input);
        assertEquals(root, output.get(0));
        
    } 
    
    // Not clear why this next test fails with an XSLException ????
    // Possible bug in Xalan-Trax impl.
    // compare with Saxon

    private String doc = 
     "<test>"
      + "<em xmlns:none=\"http://www.example.com\" />"
     + "<span xmlns:xlink='http://www.w3.org/TR/1999/xlink' xlink:type='simple' />"
     + "</test>";    
    
    
    public void testPrefixMappingIssues() 
      throws XSLException, ParsingException, IOException {
        
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        
        Builder builder = new Builder();
        Document input = builder.build(doc, "http://example.org/");
        Nodes output = xform.transform(input);
        
    }
    
    
    public void testTriple() 
      throws IOException, ParsingException, XSLException {  
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactoryTest.TripleElementFilter());

        String data = "<a><b><c/></b></a>";
        Builder builder = new Builder();
        Document doc = builder.build(data, "http://www.example.org/");
        
        Nodes result = xform.transform(doc);
        
        assertEquals(3, result.size()); 
        assertEquals(result.get(0), result.get(1));
        assertEquals(result.get(1), result.get(2));
        Element a = (Element) result.get(2);
        assertEquals("a", a.getLocalName());
        assertEquals(3, a.getChildCount());
        assertEquals(0, a.getAttributeCount());
        Element b = (Element) a.getChild(1);
        assertEquals(3, b.getChildCount());
        assertEquals("b", b.getLocalName());
        
    }
   
    
    public void testPassingNullSetsDefaultFactory() 
      throws IOException, ParsingException, XSLException {  
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactoryTest.TripleElementFilter());

        String data = "<a><b><c/></b></a>";
        Builder builder = new Builder();
        Document doc = builder.build(data, "http://www.example.org/");
        
        xform.setNodeFactory(null);        
        Nodes result = xform.transform(doc);
        
        assertEquals(1, result.size()); 
        Element a = (Element) result.get(0);
        assertEquals("a", a.getLocalName());
        assertEquals(1, a.getChildCount());
        assertEquals(0, a.getAttributeCount());
        Element b = (Element) a.getChild(0);
        assertEquals(1, b.getChildCount());
        assertEquals("b", b.getLocalName());
        
    }
    
    
    public void testTransformEmptyNodesList() 
      throws IOException, ParsingException, XSLException {  
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
       
        Nodes result = xform.transform(new Nodes());
        
        assertEquals(0, result.size());
        
    }
    
    
    public void testMinimizingFactory() 
      throws XSLException, ParsingException, IOException {
        
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactoryTest.MinimizingFactory());
        
        Builder builder = new Builder();
        Document input = builder.build("<!-- test--><test>" +
                "<em>data</em>\r\n<psan>test</psan></test>" +
                "<?target data?>", "http://example.org/");
        Nodes output = xform.transform(input);
        assertEquals(0, output.size());
        
    } 
    
    
    public void testIllegalTransform() throws XSLException {
        
        File stylesheet = new File("data/xslt/input/illegaltransform.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        Element root = new Element("root", "http://www.example.org");
        Document input = new Document(root);
        try {
            xform.transform(input);
            fail("Allowed illegal transform");
        }
        catch (XSLException ex) {
            assertNotNull(ex.getMessage());
        }
        
    }

    
    public void testElementsToAttributes() 
      throws IOException, ParsingException, XSLException {  
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new AttributeFactory());

        String data = "<a><b>in B<c>in C</c></b></a>";
        Builder builder = new Builder();
        Document doc = builder.build(data, "http://www.example.org/");
        
        Nodes result = xform.transform(doc);
        
        assertEquals(1, result.size());
        Element a = (Element) result.get(0);
        assertEquals("a", a.getLocalName());
        assertEquals(0, a.getChildCount());
        assertEquals(1, a.getAttributeCount());
        assertEquals("in B", a.getAttribute("b").getValue());
        
    }
 
    private static class AttributeFactory extends NodeFactory {

        public Nodes finishMakingElement(Element element) {
            ParentNode parent = element.getParent();
            if (parent == null || parent instanceof Document) {
                return new Nodes(element);
            }        
            return new Nodes(new Attribute(element.getQualifiedName(), 
                    element.getNamespaceURI(), element.getValue()));
        }     
        
    }
    

    public void testAttributesToElements() 
      throws IOException, ParsingException, XSLException {  
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new AttributesToElements());

        String data = "<a name='value'><b x='y' a='b'/></a>";
        Builder builder = new Builder();
        Document doc = builder.build(data, "http://www.example.org/");
        
        Nodes result = xform.transform(doc);
        
        assertEquals(1, result.size());
        Element a = (Element) result.get(0);
        assertEquals("a", a.getLocalName());
        assertEquals(2, a.getChildCount());
        assertEquals(0, a.getAttributeCount());
        Element name = (Element) a.getChild(0);
        assertEquals("name", name.getLocalName());
        assertEquals("value", name.getValue());
        Element b = (Element) a.getChild(1);
        assertEquals("b", b.getLocalName());
        assertEquals(2, b.getChildCount());
        assertEquals("y", b.getFirstChildElement("x").getValue());
        assertEquals("b", b.getFirstChildElement("a").getValue());
        
    }
 
    
    private static class AttributesToElements extends NodeFactory {

        public Nodes makeAttribute(String name, String URI, 
          String value, Attribute.Type type) {
            Element element = new Element(name, URI);
            element.appendChild(value);
            return new Nodes(element);
        }   
        
    }


    public void testCommentToAttribute() 
      throws IOException, ParsingException, XSLException {  
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);
        xform.setNodeFactory(new NodeFactory() {
            public Nodes makeComment(String text) {
                return new Nodes(new Attribute("comment", text));   
            }
        });

        String data = "<a><!--test--></a>";
        Builder builder = new Builder();
        Document doc = builder.build(data, "http://www.example.org/");
        
        Nodes result = xform.transform(doc);
        
        assertEquals(1, result.size());
        Element a = (Element) result.get(0);
        assertEquals("a", a.getLocalName());
        assertEquals(0, a.getChildCount());
        assertEquals(1, a.getAttributeCount());
        Attribute comment = a.getAttribute(0);
        assertEquals("comment", comment.getLocalName());
        assertEquals("test", comment.getValue());
        
    }

    
    // Possible bug in Xalan-Trax impl.
    //not calling startPrefixMapping????
    // compare with Saxon
    public void testAdditionalDefaultNamespace() 
      throws IOException, ParsingException, XSLException {  
        
        File stylesheet = new File("data/xslt/input/identity.xsl");
        XSLTransform xform = new XSLTransform(stylesheet);

        String data = "<pre:a xmlns:pre='http://www.example.org' xmlns='http://www.example.net'>data</pre:a>";
        Builder builder = new Builder();
        Document doc = builder.build(data, "http://www.example.org/");
        
        Nodes result = xform.transform(doc);
        
        assertEquals(1, result.size());
        Element a = (Element) result.get(0);
        assertEquals("a", a.getLocalName());
        assertEquals("pre:a", a.getQualifiedName());
        assertEquals("data", a.getValue());
        assertEquals("http://www.example.org", a.getNamespaceURI("pre"));
        assertEquals("http://www.example.net", a.getNamespaceURI(""));
        assertEquals(2, a.getNamespaceDeclarationCount());
        
    }

    
}
