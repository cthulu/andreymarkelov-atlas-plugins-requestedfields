package ru.andreymarkelov.atlas.plugins.requestedfiedls.util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.andreymarkelov.atlas.plugins.requestedfiedls.model.HttpRunnerData;
import ru.andreymarkelov.atlas.plugins.requestedfiedls.model.JSONFieldData;

public class XmlHttpRunner {
    private static final Logger log = LoggerFactory.getLogger(XmlHttpRunner.class);

    private JSONFieldData data;
    private Object defValue;

    public XmlHttpRunner(JSONFieldData data, Object defValue) {
        this.data = data;
        this.defValue = defValue;
    }

    public HttpRunnerData getData() {
        HttpRunnerData res = new HttpRunnerData();

        try {
            HttpSender httpService = new HttpSender(data.getUrl(), data.getReqType(), data.getReqDataType(), data.getUser(), data.getPassword());
            String xml = httpService.call(data.getReqData());

            List<String> vals = new ArrayList<String>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            XPathFactory xpathfactory = XPathFactory.newInstance();
            XPath xpath = xpathfactory.newXPath();
            XPathExpression expr = xpath.compile(data.getReqPath());
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.TEXT_NODE) {
                    String nodeText = node.getTextContent();
                    if (!StringUtils.isEmpty(nodeText)) {
                        vals.add(nodes.item(i).getNodeValue());
                    }
                }
            }

            if (defValue != null) {
                vals.add(0, defValue.toString());
            }

            res.setRawData(xml);
            if (vals != null) {
                if (!vals.isEmpty()) {
                    Collections.sort(vals);
                }
                res.setVals(vals);
            }
        } catch (Throwable th) {
            log.error("XmlHttpRunner::getData - error renderring", th);
            res.setError(th.getMessage());
        }

        return res;
    }
}
