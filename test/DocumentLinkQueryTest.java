import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;
import utils.LoremIpsum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentLinkQueryTest extends BlogSampleTest {

  public static String PARENT_COLLECTION_NAME = "posts2";
  public static String CHILD_COLLECTION_NAME = "comments2";
  public static String LINK_NAME = "comment";

  ObjectMapper om = new ObjectMapper();

  @Override
  public String getRouteAddress() {
    return "/admin/collection/" + PARENT_COLLECTION_NAME;
  }


  @Override
  public String getMethod() {
    return "POST";
  }


  @Override
  protected void assertContent(String s) {
    // TODO Auto-generated method stub

  }

  @Test
  public void testLinkNavigation() {
    running(
      getFakeApplication(), 
      new Runnable() 
      {
        public void run() 
        {
          int minComments = 3;
          int minPosts = 1;
          shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          TestSetup ts = prepareTest(minPosts,minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          assertTrue(ts.getAuthors().size() > 0);
          assertTrue(ts.getPostIds().size() > 0);
          assertTrue(ts.getCommentToAuthor().size() > 0);
          String postWithMoreComments = ts.getPostWithMoreComments();
          String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
          int commentCount = ts.getPostToComments().get(postWithMoreComments).size();
          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          String content = contentAsString(r);

          try {
            JsonNode node = om.readTree(content);
            String author = node.get("data").get(0).get("_author").asText();
            Assert.assertEquals(author, ts.getCommentToAuthor().get(comment));
            Assert.assertEquals(node.get("data").size(), commentCount);

          } catch (IOException e) {
            e.printStackTrace();
          }
          rq = new FakeRequest("GET", "/document/" + CHILD_COLLECTION_NAME + "/" + comment + "/comment?linkDir=from");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          content = contentAsString(r);

          try {
            JsonNode node = om.readTree(content);
            String author = node.get("data").get(0).get("_author").asText();
            String id = node.get("data").get(0).get("id").asText();
            Assert.assertEquals(author, ts.getPostToAuthors().get(postWithMoreComments));
            Assert.assertEquals(node.get("data").size(), 1);
            Assert.assertEquals(postWithMoreComments, id);
          } catch (IOException e) {
            fail("Unable to parse json");
          }
          shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
        }

      });
  }

  @Test
  public void testLinkNavigationMassive() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          int minComments = 500;
          int minPosts = 200;
          shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          TestSetup ts = prepareTest(minPosts, minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          assertTrue(ts.getAuthors().size() > 0);
          assertTrue(ts.getPostIds().size() > 0);
          assertTrue(ts.getCommentToAuthor().size() > 0);

          String postWithMoreComments = ts.getPostWithMoreComments();
          String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
          int commentCount = ts.getPostToComments().get(postWithMoreComments).size();

          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          String content = contentAsString(r);
          try {
            JsonNode node = om.readTree(content);
            String author = node.get("data").get(0).get("_author").asText();
            Assert.assertEquals(author, ts.getCommentToAuthor().get(comment));
            Assert.assertEquals(node.get("data").size(), commentCount);
            
          } catch (IOException e) {
            e.printStackTrace();
          }

          shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
        }

      });
  }

  @Test
  public void testWrongQueryParams() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          int minComments = 3;
          int minPosts = 1;
          shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          TestSetup ts = prepareTest(minPosts, minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          assertTrue(ts.getAuthors().size() > 0);
          assertTrue(ts.getPostIds().size() > 0);
          assertTrue(ts.getCommentToAuthor().size() > 0);

          String postWithMoreComments = ts.getPostWithMoreComments();
          String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
          int commentCount = ts.getPostToComments().get(postWithMoreComments).size();

          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment?linkDir=wrong");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 400, null, false);
          shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
        }

      });
  }

  @Test
  public void testWhere() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          TestSetup ts = new TestSetup();
          new AdminCollectionFunctionalTest().routeCreateCollection(PARENT_COLLECTION_NAME);
          new AdminCollectionFunctionalTest().routeCreateCollection(CHILD_COLLECTION_NAME);
          int numberOfUsers = 5;
          // Create n users
          IntStream.range(0, numberOfUsers).forEach(i -> {
            ts.addAuthor(new AdminCollectionFunctionalTest().createNewUser("user" + i));
          });
          createPosts(1, ts,PARENT_COLLECTION_NAME);
          createComments(10, ts,CHILD_COLLECTION_NAME);
          String encoding = "";
          
          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + ts.getPostWithMoreComments() + "/comment");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);

          String content = contentAsString(r);
          String commentFirstThreeWords = null;
          int count = 0;
          try {
            JsonNode node = om.readTree(content);
            Iterator<JsonNode> comments = node.get("data").iterator();
            while(comments.hasNext()){
              JsonNode n = comments.next();
              String id = n.get("id").asText();
              String comment = n.get("comment").asText();
              if (commentFirstThreeWords == null) {
                String[] split = comment.split(" ");
                commentFirstThreeWords = Joiner.on(" ").join(Lists.newArrayList(split[0], split[1], split[2]));
              }
              if (comment.indexOf(commentFirstThreeWords) > -1) {
                count++;
              }
              
            }

          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            encoding = "comment like '%<Text placeholder>%'".replace("<Text placeholder>", commentFirstThreeWords);
            encoding = URLEncoder.encode(encoding, "UTF-8");
          } catch (Exception e) {
            fail("encoding of query string failed");
          }
          rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + ts.getPostWithMoreComments() + "/comment?where="+encoding);
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          content = contentAsString(r);
          try {
            JsonNode node = om.readTree(content);

            int size = node.get("data").size();
            Assert.assertEquals(size,count);
          }catch(Exception e){
            fail("Unable to parse json");
          }
          shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
        }

      });
  }

  @Test
  public void testLinkNavigationMoreMassive() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          int minComments = 5000;
          int minPosts = 1000;
          shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          TestSetup ts = prepareTest(minPosts, minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
          assertTrue(ts.getAuthors().size() > 0);
          assertTrue(ts.getPostIds().size() > 0);
          assertTrue(ts.getCommentToAuthor().size() > 0);

          String postWithMoreComments = ts.getPostWithMoreComments();
          String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
          int commentCount = ts.getPostToComments().get(postWithMoreComments).size();
          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          String content = contentAsString(r);
          try {
            JsonNode node = om.readTree(content);
            String author = node.get("data").get(0).get("_author").asText();
            Assert.assertEquals(author, ts.getCommentToAuthor().get(comment));
            Assert.assertEquals(node.get("data").size(), commentCount);

          } catch (IOException e) {
            e.printStackTrace();
          }

          shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
        }

      });
  }

  


  


  
}
