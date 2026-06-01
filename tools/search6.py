import urllib.request
def search(query):
    url = "https://html.duckduckgo.com/html/?q=" + urllib.parse.quote(query)
    req = urllib.request.Request(
        url, data=None, headers={'User-Agent': 'Mozilla/5.0'}
    )
    try:
        html = urllib.request.urlopen(req).read().decode('utf-8')
        from html.parser import HTMLParser
        class P(HTMLParser):
            def __init__(self):
                super().__init__()
                self.in_a = False
                self.results = []
                self.cur = ""
            def handle_starttag(self, tag, attrs):
                if tag == 'a' and ('class', 'result__snippet') in attrs: self.in_a = True
            def handle_endtag(self, tag):
                if tag == 'a' and self.in_a:
                    self.in_a = False; self.results.append(self.cur); self.cur = ""
            def handle_data(self, data):
                if self.in_a: self.cur += data
        p = P()
        p.feed(html)
        for i, res in enumerate(p.results[:5]): print(f"{i+1}. {res.strip()}")
    except Exception as e: print("Error:", e)
search("android opengl disable vsync swapinterval egl")
