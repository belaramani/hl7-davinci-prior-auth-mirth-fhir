package medibridgepayermock.medibridgepayermock;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SmartSecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (SmartAuthController.isValidToken(token)) {
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/fhir+json");
        response.getWriter().write("""
                {
                  "resourceType": "OperationOutcome",
                  "issue": [
                    {
                      "severity": "error",
                      "code": "security",
                      "diagnostics": "SMART on FHIR OAuth 2.0 Bearer token missing, invalid, or expired. Obtain a token from POST /oauth2/token."
                    }
                  ]
                }
                """);
        return false;
    }
}