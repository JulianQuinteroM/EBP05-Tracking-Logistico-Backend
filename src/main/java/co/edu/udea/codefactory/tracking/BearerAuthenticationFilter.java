package co.edu.udea.codefactory.tracking;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerAuthenticationFilter extends OncePerRequestFilter {
    private final Accounts accounts;
    public BearerAuthenticationFilter(Accounts accounts) { this.accounts=accounts; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String auth=req.getHeader("Authorization");
        if(auth!=null && auth.startsWith("Bearer ")) {
            UserDetails user=accounts.tokenUser(auth.substring(7));
            if(user==null) { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"Sesión caducada o acceso revocado\"}"); return; }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getUsername(),null,user.getAuthorities()));
        }
        chain.doFilter(req,res);
    }
}
