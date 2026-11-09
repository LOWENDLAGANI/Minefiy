const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.addAdminRole = functions.https.onCall((data, context) => {
  // Check if request is made by an existing admin (optional but recommended)
  // This initial check can be skipped for the very first admin.
  // if (context.auth.token.isAdmin !== true) {
  //   return { error: "Only admins can add other admins." };
  // }

  // Get user and add custom claim (admin)
  return admin.auth().getUserByEmail(data.email).then((user) => {
    return admin.auth().setCustomUserClaims(user.uid, {
      isAdmin: true,
    });
  }).then(() => {
    return {
      message: `Success! ${data.email} has been made an admin.`,
    };
  }).catch((err) => {
    return err;
  });
});