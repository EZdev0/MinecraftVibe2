import urllib.request

def search(query):
    url = "https://html.duckduckgo.com/html/?q=" + urllib.parse.quote(query)
    req = urllib.request.Request(
        url,
        data=None,
        headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
    )
    try:
        response = urllib.request.urlopen(req)
        html = response.read().decode('utf-8')
        from html.parser import HTMLParser
        class MyHTMLParser(HTMLParser):
            def __init__(self):
                super().__init__()
                self.in_a = False
                self.results = []
                self.current_snippet = ""
            def handle_starttag(self, tag, attrs):
                if tag == 'a' and ('class', 'result__snippet') in attrs:
                    self.in_a = True
            def handle_endtag(self, tag):
                if tag == 'a' and self.in_a:
                    self.in_a = False
                    self.results.append(self.current_snippet)
                    self.current_snippet = ""
            def handle_data(self, data):
                if self.in_a:
                    self.current_snippet += data
        parser = MyHTMLParser()
        parser.feed(html)
        for i, res in enumerate(parser.results[:5]):
            print(f"{i+1}. {res.strip()}")
    except Exception as e:
        print("Error:", e)

search("eglSwapInterval GLSurfaceView android java")
