# [URL-Shortener](https://app.hrva.cc)

## Overview
Welcome to the URL-Shortener project! This application allows you to shorten URLs efficiently and securely with additional features such as API keys, custom URLs, and comprehensive link management.

## Features
- **API Keys for External Access**: Generate API keys to access the URL shortening service programmatically.
- **Automatic Link Deactivation**: Links can automatically deactivate after a certain time or after being accessed by a set number of unique users.+
- **Custom Expiration Dates**: Allow users to set custom expiration dates for their shortened URLs.
- **Dashboard**: View all URLs, their access counts, and more in a user-friendly dashboard.
- **Custom Short URLs**: Logged-in users can create custom short URLs; non-logged-in users receive a randomly generated URL.
- **Google Safe Browsing Integration**: URLs are scanned using Google Safe Browsing to ensure they are not malicious.
- **URL Validation**: Regular users can validate URLs through an external site to ensure safety.
- **Login Security**: Accounts are locked after multiple failed login attempts to prevent unauthorized access.

## Roadmap
1. **SSO Logins**: Integrate single sign-on (SSO) options with GitHub, Google, etc.
2. **Frontend Redesign**: Overhaul the frontend with a new design for improved user experience.
3. **Backend Refactor**: Refactor the backend to Java 21, adhering to best practices.
4. **Easy Deploy**: Streamline the deployment process for ease of use.
5. **Caching Database**: Implement database caching to provide faster responses.
6. **Improved API Documentation**: Enhance the API documentation for better clarity and usability.

## Technologies Used
- **Backend**: Java, Spring Boot
- **Frontend**: Next
- **Database**: Postgres

## Upcoming Features Suggestions in Review
- **QR Code Generation**: Automatically generate QR codes for each shortened URL for easy sharing.
- **User Roles and Permissions**: Implement user roles (e.g., admin, editor, viewer) with different levels of access and control.
- **Two-Factor Authentication (2FA)**: Enhance security by adding two-factor authentication for user logins.
- **URL Categorization**: Allow users to categorize and tag their URLs for better organization.
- **Bulk URL Shortening**: Enable users to shorten multiple URLs at once through batch processing.
- **Link Preview**: Show a preview of the destination website before redirection.
- **Integration with Other Services**: Integrate with popular services like Slack, Discord, and Microsoft Teams for easier sharing and notifications.
- **Dark Mode**: Add a dark mode option for the dashboard to reduce eye strain and improve usability in low-light environments.

## Contribution
We welcome contributions from the community!

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact
For questions, issues, or feature requests, please open an issue on GitHub or contact me directly via [email](mailto:url-shortener@hrva.cc).

Thank you for using URL-Shortener or just viewing this page!
