/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */
package org.apache.wiki.auth.user;
import java.io.Serializable;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.user.DuplicateUserException;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.apache.wiki.util.CryptoUtil;

import junit.framework.TestCase;




/**
 */
public class XMLUserDatabaseTest extends TestCase
{

  private XMLUserDatabase m_db;
  
  private TestEngine m_engine = null;

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception
  {
      super.setUp();
      Properties props = new Properties();
      props.load( TestEngine.findTestProperties() );
      m_engine  = new TestEngine(props);
      m_db = new XMLUserDatabase();
      m_db.initialize(m_engine, props);
  }
  
  protected void tearDown() throws Exception
  {
      try
      {
          // If this fails, shutdown() is never called unless it's wrapped in a finally block.
          assertEquals( 8, m_db.getWikiNames().length );
      }
      finally
      {
          super.tearDown();
          m_engine.shutdown();
      }
  }

  public void testDeleteByLoginName() throws WikiSecurityException
  {
      // First, count the number of users in the db now.
      int oldUserCount = m_db.getWikiNames().length;

      // Create a new user with random name
      String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
      UserProfile profile = m_db.newProfile();
      profile.setEmail("testuser@testville.com");
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword("password");
      m_db.save(profile);

      // Make sure the profile saved successfully
      profile = m_db.findByLoginName( loginName );
      assertEquals( loginName, profile.getLoginName() );
      assertEquals( oldUserCount+1, m_db.getWikiNames().length );

      // Now delete the profile; should be back to old count
      m_db.deleteByLoginName( loginName );
      assertEquals( oldUserCount, m_db.getWikiNames().length );
  }

  public void testAttributes() throws Exception
  {
      UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );
      
      Map<String,Serializable> attributes = profile.getAttributes();
      assertEquals( 2, attributes.size() );
      assertTrue( attributes.containsKey( "attribute1" ) );
      assertTrue( attributes.containsKey( "attribute2" ) );
      assertEquals( "some random value", attributes.get( "attribute1" ) );
      assertEquals( "another value", attributes.get( "attribute2" ) );
      
      // Change attribute 1, and add another one
      attributes.put( "attribute1", "replacement value" );
      attributes.put( "attribute the third", "some value" );
      m_db.save( profile );
      
      // Retrieve the profile again and make sure our values got saved
      profile = m_db.findByEmail( "janne@ecyrd.com" );
      attributes = profile.getAttributes();
      assertEquals( 3, attributes.size() );
      assertTrue( attributes.containsKey( "attribute1" ) );
      assertTrue( attributes.containsKey( "attribute2" ) );
      assertTrue( attributes.containsKey( "attribute the third" ) );
      assertEquals( "replacement value", attributes.get( "attribute1" ) );
      assertEquals( "another value", attributes.get( "attribute2" ) );
      assertEquals( "some value", attributes.get( "attribute the third" ) );
      
      // Restore the original attributes and re-save
      attributes.put( "attribute1", "some random value" );
      attributes.remove( "attribute the third" );
      m_db.save( profile );
  }

  public void testFindByEmail()
  {
    try
    {
        UserProfile profile = m_db.findByEmail("janne@ecyrd.com");
        assertEquals( "-7739839977499061014", profile.getUid() );
        assertEquals("janne",           profile.getLoginName());
        assertEquals("Janne Jalkanen",  profile.getFullname());
        assertEquals("JanneJalkanen",   profile.getWikiName());
        assertEquals("{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword());
        assertEquals("janne@ecyrd.com", profile.getEmail());
    }
    catch (NoSuchPrincipalException e)
    {
        assertTrue(false);
    }
    try
    {
        m_db.findByEmail("foo@bar.org");
        // We should never get here
        assertTrue(false);
    }
    catch (NoSuchPrincipalException e)
    {
        assertTrue(true);
    }
  }

  public void testFindByFullName()
  {
      try
      {
          UserProfile profile = m_db.findByFullName( "Janne Jalkanen" );
          assertEquals( "-7739839977499061014", profile.getUid() );
          assertEquals( "janne", profile.getLoginName() );
          assertEquals( "Janne Jalkanen", profile.getFullname() );
          assertEquals( "JanneJalkanen", profile.getWikiName() );
          assertEquals("{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword());
          assertEquals( "janne@ecyrd.com", profile.getEmail() );
          assertNotNull( profile.getCreated() );
          assertNotNull( profile.getLastModified() );
      }
      catch( NoSuchPrincipalException e )
      {
          assertTrue( false );
      }
      try
      {
          m_db.findByEmail( "foo@bar.org" );
          // We should never get here
          assertTrue( false );
      }
      catch( NoSuchPrincipalException e )
      {
          assertTrue( true );
      }
  }

  public void testFindByUid()
  {
      try
      {
          UserProfile profile = m_db.findByUid( "-7739839977499061014" );
          assertEquals( "-7739839977499061014", profile.getUid() );
          assertEquals( "janne", profile.getLoginName() );
          assertEquals( "Janne Jalkanen", profile.getFullname() );
          assertEquals( "JanneJalkanen", profile.getWikiName() );
          assertEquals("{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword());
          assertEquals( "janne@ecyrd.com", profile.getEmail() );
          assertNotNull( profile.getCreated() );
          assertNotNull( profile.getLastModified() );
      }
      catch( NoSuchPrincipalException e )
      {
          assertTrue( false );
      }
      try
      {
          m_db.findByEmail( "foo@bar.org" );
          // We should never get here
          assertTrue( false );
      }
      catch( NoSuchPrincipalException e )
      {
          assertTrue( true );
      }
  }
  
  public void testFindByWikiName()
  {
      try
      {
          UserProfile profile = m_db.findByWikiName("JanneJalkanen");
          assertEquals( "-7739839977499061014", profile.getUid() );
          assertEquals("janne",           profile.getLoginName());
          assertEquals("Janne Jalkanen",  profile.getFullname());
          assertEquals("JanneJalkanen",   profile.getWikiName());
          assertEquals("{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword());
          assertEquals("janne@ecyrd.com", profile.getEmail());
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(false);
      }
      try
      {
          m_db.findByEmail("foo");
          // We should never get here
          assertTrue(false);
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(true);
      }
    }

  public void testFindByLoginName()
  {
      try
      {
          UserProfile profile = m_db.findByLoginName("janne");
          assertEquals( "-7739839977499061014", profile.getUid() );
          assertEquals("janne",           profile.getLoginName());
          assertEquals("Janne Jalkanen",  profile.getFullname());
          assertEquals("JanneJalkanen",   profile.getWikiName());
          assertEquals("{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword());
          assertEquals("janne@ecyrd.com", profile.getEmail());
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(false);
      }
      try
      {
          m_db.findByEmail("FooBar");
          // We should never get here
          assertTrue(false);
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(true);
      }
    }

  public void testGetWikiNames() throws WikiSecurityException
  {
      // There are 8 test users in the database
      Principal[] p = m_db.getWikiNames();
      assertEquals( 8, p.length );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "Administrator", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.ALICE, WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BOB, WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.CHARLIE, WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "FredFlintstone", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BIFF, WikiPrincipal.WIKI_NAME ) ) );
  }

  public void testRename() throws Exception
  {
      // Try renaming a non-existent profile; it should fail
      try
      {
          m_db.rename( "nonexistentname", "renameduser" );
          fail( "Should not have allowed rename..." );
      }
      catch ( NoSuchPrincipalException e )
      {
          // Cool; that's what we expect
      }

      // Create new user & verify it saved ok
      UserProfile profile = m_db.newProfile();
      profile.setEmail( "renamed@example.com" );
      profile.setFullname( "Renamed User" );
      profile.setLoginName( "olduser" );
      profile.setPassword( "password" );
      m_db.save( profile );
      profile = m_db.findByLoginName( "olduser" );
      assertNotNull( profile );

      // Try renaming to a login name that's already taken; it should fail
      try
      {
          m_db.rename( "olduser", "janne" );
          fail( "Should not have allowed rename..." );
      }
      catch ( DuplicateUserException e )
      {
          // Cool; that's what we expect
      }

      // Now, rename it to an unused name
      m_db.rename( "olduser", "renameduser" );

      // The old user shouldn't be found
      try
      {
          profile = m_db.findByLoginName( "olduser" );
          fail( "Old user was found, but it shouldn't have been." );
      }
      catch ( NoSuchPrincipalException e )
      {
          // Cool, it's gone
      }

      // The new profile should be found, and its properties should match the old ones
      profile = m_db.findByLoginName( "renameduser" );
      assertEquals( "renamed@example.com", profile.getEmail() );
      assertEquals( "Renamed User", profile.getFullname() );
      assertEquals( "renameduser", profile.getLoginName() );
      assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );

      // Delete the user
      m_db.deleteByLoginName( "renameduser" );
  }

  public void testSave() throws Exception
  {
      try
      {
          UserProfile profile = m_db.newProfile();
          profile.setEmail("user@example.com");
          profile.setLoginName("user");
          profile.setPassword("password");
          m_db.save(profile);
          profile = m_db.findByEmail("user@example.com");
          assertEquals("user@example.com", profile.getEmail());
          assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );
          
          // Make sure we can find it by uid
          String uid = profile.getUid();
          assertNotNull( m_db.findByUid( uid ) );
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(false);
      }
      catch (WikiSecurityException e)
      {
          assertTrue(false);
      }
  }

  public void testValidatePassword()
  {
      assertFalse(m_db.validatePassword("janne", "test"));
      assertTrue(m_db.validatePassword("janne", "myP@5sw0rd"));
      assertTrue(m_db.validatePassword("user", "password"));
  }

}
