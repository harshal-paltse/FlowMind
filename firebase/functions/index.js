const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

// 1. Send FCM Notification
exports.sendNotification = functions.https.onCall(async (data, context) => {
    const targetToken = data.token;
    const title = data.title;
    const body = data.body;

    if (!targetToken) {
        throw new functions.https.HttpsError("invalid-argument", "Token is required");
    }

    const message = {
        notification: {
            title: title || "FlowMind Alert",
            body: body || "Your workflow has updates.",
        },
        token: targetToken,
    };

    try {
        const response = await admin.messaging().send(message);
        return { success: true, messageId: response };
    } catch (error) {
        throw new functions.https.HttpsError("internal", error.message);
    }
});

// 2. Send Email Summary (Using Nodemailer)
const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
        user: "flowmind.noreply@gmail.com",
        pass: process.env.EMAIL_PASSWORD || "dummy_password"
    }
});

exports.sendEmailSummary = functions.https.onCall(async (data, context) => {
    const email = data.email;
    const workflowName = data.workflowName;
    const summary = data.summary;

    if (!email) {
        throw new functions.https.HttpsError("invalid-argument", "Email is required");
    }

    const mailOptions = {
        from: "FlowMind <flowmind.noreply@gmail.com>",
        to: email,
        subject: `FlowMind Workflow Summary: ${workflowName}`,
        text: `Here is the summary of your recent workflow execution:\n\n${summary}`
    };

    try {
        await transporter.sendMail(mailOptions);
        return { success: true };
    } catch (error) {
        throw new functions.https.HttpsError("internal", error.message);
    }
});
