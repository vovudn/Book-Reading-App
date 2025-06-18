// /**
//  * Import function triggers from their respective submodules:
//  *
//  * const {onCall} = require("firebase-functions/v2/https");
//  * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
//  *
//  * See a full list of supported triggers at https://firebase.google.com/docs/functions
//  */

// const {onRequest} = require("firebase-functions/v2/https");
// const logger = require("firebase-functions/logger");

// // Create and deploy your first functions
// // https://firebase.google.com/docs/functions/get-started

// // exports.helloWorld = onRequest((request, response) => {
// //   logger.info("Hello logs!", {structuredData: true});
// //   response.send("Hello from Firebase!");
// // });

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotificationOnNewBook = functions.database
  .ref("/Books/{bookId}")
  .onCreate(async (snapshot, context) => {
    const book = snapshot.val();
    const bookTitle = book.title;

    const message = {
      topic: "all",
      notification: {
        title: "New Book Added!",
        body: `Check out "${bookTitle}" now!`
      },
      data: {
        bookId: book.id || ""
      }
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("✅ Notification sent:", response);
    } catch (error) {
      console.error("❌ Error sending notification:", error);
    }
  });

