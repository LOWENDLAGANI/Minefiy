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
fun TermsOfServiceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Terms of Service",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Last Updated: [Date]", // *** REPLACE WITH ACTUAL DATE ***
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        TermsOfServiceSection(
            title = "1. Acceptance of Terms",
            content = "By accessing or using the Minefiy mobile application (the 'Service'), you agree to be bound by these Terms of Service (the 'Terms'). If you disagree with any part of the terms then you may not access the Service."
        )
        TermsOfServiceSection(
            title = "2. User Accounts",
            content = "When you create an account with us, you must provide us information that is accurate, complete, and current at all times. Failure to do so constitutes a breach of the Terms, which may result in immediate termination of your account on our Service.\n\nYou are responsible for safeguarding the password that you use to access the Service and for any activities or actions under your password, whether your password is with our Service or a third-party service.\n\nYou agree not to disclose your password to any third party. You must notify us immediately upon becoming aware of any breach of security or unauthorized use of your account."
        )
        TermsOfServiceSection(
            title = "3. Intellectual Property",
            content = "The Service and its original content (excluding content provided by users), features and functionality are and will remain the exclusive property of Minetallest and its licensors."
        )
        TermsOfServiceSection(
            title = "4. User-Generated Content",
            content = "Our Service allows you to post, link, store, share and otherwise make available certain information, text, graphics, videos, or other material ('Content'). You are responsible for the Content that you post on or through the Service, including its legality, reliability, and appropriateness."
        )
        TermsOfServiceSection(
            title = "5. Prohibited Uses",
            content = "You agree not to use the Service: (a) in any way that violates any applicable national or international law or regulation; (b) for the purpose of exploiting, harming, or attempting to exploit or harm minors in any way; (c) to transmit, or procure the sending of, any advertising or promotional material, including any 'junk mail', 'chain letter,' 'spam,' or any other similar solicitation; (d) to impersonate or attempt to impersonate Minetallest, a Minetallest employee, another user, or any other person or entity."
        )
        TermsOfServiceSection(
            title = "6. Termination",
            content = "We may terminate or suspend your account immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms."
        )
        TermsOfServiceSection(
            title = "7. Links to Other Websites",
            content = "Our Service may contain links to third-party web sites or services that are not owned or controlled by Minetallest.\n\nMinetallest has no control over, and assumes no responsibility for, the content, privacy policies, or practices of any third party web sites or services. You further acknowledge and agree that Minetallest shall not be responsible or liable, directly or indirectly, for any damage or loss caused or alleged to be caused by or in connection with use of or reliance on any such content, goods or services available on or through any such web sites or services."
        )
        TermsOfServiceSection(
            title = "8. Disclaimer of Warranties",
            content = "The Service is provided on an 'AS IS' and 'AS AVAILABLE' basis. Minetallest makes no representations or warranties of any kind, express or implied, as to the operation of their services, or the information, content or materials included therein."
        )
        TermsOfServiceSection(
            title = "9. Limitation of Liability",
            content = "In no event shall Minetallest, nor its directors, employees, partners, agents, suppliers, or affiliates, be liable for any indirect, incidental, special, consequential or punitive damages, including without limitation, loss of profits, data, use, goodwill, or other intangible losses, resulting from (i) your access to or use of or inability to access or use the Service; (ii) any conduct or content of any third party on the Service; (iii) any content obtained from the Service; and (iv) unauthorized access, use or alteration of your transmissions or content, whether based on warranty, contract, tort (including negligence) or any other legal theory, whether or not we have been informed of the possibility of such damage, and even if a remedy set forth herein is found to have failed of its essential purpose."
        )
        TermsOfServiceSection(
            title = "10. Governing Law",
            content = "These Terms shall be governed and construed in accordance with the laws of [Your Jurisdiction], without regard to its conflict of law provisions."
        )
        TermsOfServiceSection(
            title = "11. Changes to Terms",
            content = "We reserve the right, at our sole discretion, to modify or replace these Terms at any time. If a revision is material we will try to provide at least 30 days notice prior to any new terms taking effect. What constitutes a material change will be determined at our sole discretion."
        )
        TermsOfServiceSection(
            title = "12. Contact Us",
            content = "If you have any questions about these Terms, please contact us at: [Your Contact Email/Method]"
        )
    }
}

@Composable
fun TermsOfServiceSection(title: String, content: String) {
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
fun PreviewTermsOfServiceScreen() {
    TermsOfServiceScreen()
}
