package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Session> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry).withWriteConcern(WriteConcern.MAJORITY);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry).withWriteConcern(WriteConcern.MAJORITY);
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
        log.info("addUser {}", user.getEmail());
        try {
            usersCollection.insertOne(user);
            return true;
        } catch (MongoException mongoException) {
            log.error(mongoException.getMessage(), mongoException);
            return false;
        }
        //TODO > Ticket: Handling Errors - make sure to only add new users
        // and not users that already exist.

    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        //TODO> Ticket: User Management - implement the method that allows session information to be
        // stored in it's designated collection.
        Session session = new Session();
        session.setUserId(userId);
        session.setJwt(jwt);
        try {
            if (sessionsCollection.find(eq("jwt", jwt)).first() == null &&
                    sessionsCollection.find(eq("user_id", userId)).first() == null) {
                sessionsCollection.insertOne(session);
                return true;
            }
            return false;
        } catch (MongoException mongoException) {
            log.error(mongoException.getMessage(), mongoException);
            throw new IncorrectDaoOperation(mongoException.getMessage());
        }
        //TODO > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        log.info("getUser: {}", email);
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        return usersCollection.find(eq("email", email)).first();
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        log.info("getUserSession: {}", userId);
        //TODO> Ticket: User Management - implement the method that returns Sessions for a given
        // userId
        return sessionsCollection.find(eq("user_id", userId)).first();
    }

    public boolean deleteUserSessions(String userId) {
        log.info("deleteUserSessions: {}", userId);
        //TODO> Ticket: User Management - implement the delete user sessions method
        try {
            sessionsCollection.deleteMany(eq("user_id", userId));
            return true;
        } catch (MongoException mongoException) {
            log.error(mongoException.getMessage(), mongoException);
            return false;
        }
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        log.info("deleteUser: {}", email);
        // remove user sessions
        //TODO> Ticket: User Management - implement the delete user method
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.
        try {
            DeleteResult userDeleteResult = usersCollection.deleteMany(eq("email", email));
            DeleteResult sessionDeleteResult = sessionsCollection.deleteMany(eq("user_id", email));
            return userDeleteResult.getDeletedCount() > 0 && sessionDeleteResult.getDeletedCount() > 0;
        } catch (MongoException exception) {
            log.error(exception.getMessage(), exception);
            return false;
        }
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        log.info("updateUserPreferences: {}", email);
        //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating an entry.
        try {
            if (userPreferences != null) {
                UpdateResult userUpdateResult = usersCollection.updateOne(
                        eq("email", email),
                        set("preferences", userPreferences)
                );
                return userUpdateResult.getModifiedCount() > 0;
            } else {
                throw new IncorrectDaoOperation("Cannot save null");
            }
        } catch (MongoException mongoException) {
            return false;
        }
    }
}
