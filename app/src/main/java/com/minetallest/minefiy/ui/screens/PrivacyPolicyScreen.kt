package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Last Updated: [Date]", // *** REPLACE WITH ACTUAL DATE ***
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        PrivacyPolicySection(
            title = "1. Introduction",
            content = "Welcome to Minefiy! This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you visit our mobile application. Please read this Privacy Policy carefully. IF YOU DO NOT AGREE WITH THE TERMS OF THIS PRIVACY POLICY, PLEASE DO NOT ACCESS THE APPLICATION."
        )
        PrivacyPolicySection(
            title = "2. Information We Collect",
            content = "We may collect information about you in a variety of ways. The information we may collect via the Application depends on the content and materials you use, and includes:\n\n*   **Personal Data:** Personally identifiable information, such as your name, email address, and demographic information, that you voluntarily give to us when you register with the Application or when you choose to participate in various activities related to the Application.\n*   **Derivative Data:** Information our servers automatically collect when you access the Application, such as your IP address, your browser type, your operating system, your access times, and the pages you have viewed directly before and after accessing the Application.\n*   **Financial Data:** Financial information, such as data related to your payment method (e.g., valid credit card number, card brand, expiration date) that we may collect if you make purchases, from our Application. [Note: If you handle payments directly, this section needs significant detail. If using a third-party like Google Play Billing, clarify that.]\n*   **Mobile Device Data:** Device information, such as your mobile device ID, model, and manufacturer, and information about the location of your device, if you permit our mobile application to access location information.\n*   **Third-Party Data:** Information from third parties, such as personal information or network data, if you connect your account to the third party and grant the Application permission to access this information."
        )
        PrivacyPolicySection(
            title = "3. How We Use Your Information",
            content = "Having accurate information about you permits us to provide you with a smooth, efficient, and customized experience. Specifically, we may use information collected about you via the Application to:\n\n*   Create and manage your account.\n*   Deliver targeted advertising to you. (Via Google AdMob)\n*   Enable user-to-user communications.\n*   Fulfill and manage purchases, orders, payments, and other transactions related to the Application.\n*   Generate a personal profile about you to make your visit to the Application more personalized.\n*   Increase the efficiency and operation of the Application.\n*   Monitor and analyze usage and trends to improve your experience with the Application.\n*   Notify you of updates to the Application.\n*   Offer new products, services, mobile applications, and/or recommendations to you.\n*   Perform other business activities as needed.\n*   Request feedback and contact you about your use of the Application.\n*   Resolve disputes and troubleshoot problems.\n*   Respond to product and customer service requests.\n*   Send you a newsletter or other updates."
        )
        PrivacyPolicySection(
            title = "4. Disclosure of Your Information",
            content = "We may share information we have collected about you in certain situations. Your information may be disclosed as follows:\n\n*   **By Law or to Protect Rights:** If we believe the release of information about you is necessary to respond to legal process, to investigate or remedy potential violations of our policies, or to protect the rights, property, and safety of others, we may share your information as permitted or required by any applicable law, rule, or regulation.\n*   **Third-Party Service Providers:** We may share your information with third parties that perform services for us or on our behalf, including data analysis, email delivery, hosting services, customer service, and marketing assistance. (e.g., Firebase, AWS S3, Google AdMob)"
        )
        PrivacyPolicySection(
            title = "5. Security of Your Information",
            content = "We use administrative, technical, and physical security measures to help protect your personal information. While we have taken reasonable steps to secure the personal information you provide to us, please be aware that despite our efforts, no security measures are perfect or impenetrable, and no method of data transmission can be guaranteed against any interception or other type of misuse."
        )
        PrivacyPolicySection(
            title = "6. Policy for Children",
            content = "We do not knowingly solicit information from or market to children under the age of 13. If you become aware of any data we have collected from children under age 13, please contact us using the contact information provided below."
        )
        PrivacyPolicySection(
            title = "7. Your Rights (e.g., GDPR, CCPA if applicable)",
            content = "Depending on your location, you may have certain rights regarding your personal data, such as the right to access, correct, delete, or restrict processing of your data. Please contact us to exercise these rights."
        )
        PrivacyPolicySection(
            title = "8. Contact Us",
            content = "If you have questions or comments about this Privacy Policy, please contact us at: [Your Contact Email/Method]"
        )
    }
}

@Composable
fun PrivacyPolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPrivacyPolicyScreen() {
    PrivacyPolicyScreen()
}
