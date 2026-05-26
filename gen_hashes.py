import bcrypt
users = ['admin','alice','bob','charlie','diana','emma','frank','george','hannah','ivan','jana','kent','luka','mira','nino','olga','petar','sasa','tereza','viktor']
for u in users:
    pwd = (u + '123').encode()
    print(u, bcrypt.hashpw(pwd, bcrypt.gensalt()).decode())
